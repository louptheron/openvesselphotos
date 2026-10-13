package org.openvesselphotos.infrastructure.api

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.TestImages
import org.openvesselphotos.config.AdminAuthentication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest(properties = ["openvesselphotos.admin-password=test-password"])
@AutoConfigureMockMvc
@Testcontainers
class PublicAndAdminApiITests {
    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("openvesselphotos.database.url") {
                "postgres://${postgres.username}:${postgres.password}@${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}"
            }
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    private fun adminCookie(): Cookie {
        val response =
            mockMvc
                .perform(
                    post(
                        "/v1/admin/login",
                    ).contentType(MediaType.APPLICATION_JSON).content("""{"password":"test-password"}"""),
                ).andExpect(status().isNoContent)
                .andReturn()
                .response

        return requireNotNull(response.getCookie(AdminAuthentication.COOKIE_NAME))
    }

    private fun submitPhoto(
        keyType: String,
        key: String,
        vesselName: String? = null,
    ) {
        val request =
            multipart("/v1/$keyType/$key/photos")
                .header("Sec-Fetch-Site", "same-origin")
                .file(MockMultipartFile("file", "boat.jpg", "image/jpeg", TestImages.create(1200, 800)))
                .param("credit", "J. Le Bihan")
                .param("licence", "CC-BY-4.0")
                .param("takenOn", "2026-06-01")
        vesselName?.let { request.param("vesselName", it) }

        mockMvc.perform(request).andExpect(status().isCreated).andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    fun `A submitted photo Should be public under both keys once approved and linked`() {
        submitPhoto("cfr", "FRA000123456", "Ker Avel")

        // Not public before review
        mockMvc.perform(get("/v1/cfr/FRA000123456")).andExpect(status().isNotFound)

        // The admin approves it
        mockMvc.perform(get("/v1/admin/queue")).andExpect(status().isUnauthorized)
        val cookie = adminCookie()
        val queue =
            mockMvc
                .perform(get("/v1/admin/queue").cookie(cookie))
                .andExpect(status().isOk)
                .andExpect(
                    jsonPath(
                        "$.photos[?(@.photo.submittedKey == 'FRA000123456')].vessel",
                    ).value(org.hamcrest.Matchers.contains(null as Any?)),
                ).andReturn()
                .response.contentAsString
        val photoId =
            Regex(
                """"id":(\d+),"number":null,"submittedKeyType":"cfr","submittedKey":"FRA000123456"""",
            ).find(queue)!!.groupValues[1]
        mockMvc.perform(get("/v1/admin/photos/$photoId.jpg").cookie(cookie)).andExpect(status().isOk)
        mockMvc.perform(post("/v1/admin/photos/$photoId/approve").cookie(cookie)).andExpect(status().isNoContent)

        mockMvc
            .perform(get("/v1/cfr/FRA000123456"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Ker Avel"))
            .andExpect(jsonPath("$.flag").value("FRA"))
            .andExpect(jsonPath("$.photos[0].n").value(1))
            .andExpect(jsonPath("$.photos[0].licence").value("CC-BY-4.0"))
            .andExpect(jsonPath("$.photos[0].takenOn").value("2026-06-01"))
            .andExpect(jsonPath("$.photos[0].urls.cfr").value("http://localhost/v1/cfr/FRA000123456/photos/1.jpg"))
            .andExpect(jsonPath("$.photos[0].urls.mmsi").doesNotExist())
        mockMvc
            .perform(get("/v1/cfr/FRA000123456.jpg").param("size", "320"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))

        // Someone suggests the MMSI; once approved, the same photo is served under it
        mockMvc
            .perform(
                post("/v1/cfr/FRA000123456/link")
                    .header("Sec-Fetch-Site", "same-origin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"keyType":"mmsi","key":"227000101","contributor":"A. Morvan"}"""),
            ).andExpect(status().isCreated)
        val linkId =
            Regex(""""keyLinks":\[\{"id":(\d+)""")
                .find(
                    mockMvc
                        .perform(get("/v1/admin/queue").cookie(cookie))
                        .andReturn()
                        .response.contentAsString,
                )!!
                .groupValues[1]
        mockMvc.perform(post("/v1/admin/links/$linkId/approve").cookie(cookie)).andExpect(status().isNoContent)

        mockMvc.perform(get("/v1/mmsi/227000101/photos/1.jpg")).andExpect(status().isOk)
        // History events carry their type and details, for clients to translate them
        mockMvc
            .perform(get("/v1/mmsi/227000101"))
            .andExpect(
                jsonPath(
                    "$.history[?(@.type == 'KEY_LINKED')].key",
                ).value(org.hamcrest.Matchers.contains("MMSI 227000101")),
            ).andExpect(
                jsonPath(
                    "$.history[?(@.type == 'KEY_LINKED')].linkedTo",
                ).value(org.hamcrest.Matchers.contains("CFR FRA000123456")),
            ).andExpect(
                jsonPath(
                    "$.history[?(@.type == 'PHOTO_PUBLISHED')].photoNumber",
                ).value(org.hamcrest.Matchers.contains(1)),
            )
        mockMvc
            .perform(get("/v1/search").param("q", "ker"))
            .andExpect(jsonPath("$[0].mmsi").value("227000101"))
            .andExpect(jsonPath("$[0].mainPhoto.urls.mmsi").value("http://localhost/v1/mmsi/227000101/photos/1.jpg"))
        mockMvc.perform(get("/v1/vessels/recent")).andExpect(jsonPath("$[0].cfr").value("FRA000123456"))

        // A report removes the photo
        mockMvc
            .perform(
                post("/v1/mmsi/227000101/photos/1/reports")
                    .header("Sec-Fetch-Site", "same-origin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reason":"WRONG_VESSEL"}"""),
            ).andExpect(status().isCreated)
        val reportId =
            Regex(""""reports":\[\{"id":(\d+)""")
                .find(
                    mockMvc
                        .perform(get("/v1/admin/queue").cookie(cookie))
                        .andReturn()
                        .response.contentAsString,
                )!!
                .groupValues[1]
        mockMvc.perform(post("/v1/admin/reports/$reportId/remove-photo").cookie(cookie)).andExpect(status().isNoContent)
        mockMvc.perform(get("/v1/cfr/FRA000123456/photos/1.jpg")).andExpect(status().isNotFound)
    }

    @Test
    fun `Invalid requests Should return explicit errors`() {
        mockMvc
            .perform(
                get("/v1/mmsi/12"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_KEY"))
        mockMvc.perform(get("/v1/mmsi/999999999")).andExpect(status().isNotFound)
        mockMvc
            .perform(
                multipart("/v1/mmsi/227000555/photos")
                    .header("Sec-Fetch-Site", "same-origin")
                    .file(MockMultipartFile("file", "x.jpg", "image/jpeg", "nope".toByteArray()))
                    .param("credit", "Me")
                    .param("licence", "CC0-1.0"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE"))
        mockMvc
            .perform(
                post("/v1/admin/login").contentType(MediaType.APPLICATION_JSON).content("""{"password":"wrong"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `Pages Should be rendered for search engines`() {
        // A vessel whose name tries to inject HTML and a script
        submitPhoto("cfr", "FRA000999888", "</script><b>Evil</b>")
        val cookie = adminCookie()
        val queue =
            mockMvc
                .perform(get("/v1/admin/queue").cookie(cookie))
                .andReturn()
                .response.contentAsString
        val photoId =
            Regex(
                """"id":(\d+),"number":null,"submittedKeyType":"cfr","submittedKey":"FRA000999888"""",
            ).find(queue)!!.groupValues[1]
        mockMvc.perform(post("/v1/admin/photos/$photoId/approve").cookie(cookie)).andExpect(status().isNoContent)

        val html =
            mockMvc
                .perform(get("/vessels/cfr/FRA000999888"))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn()
                .response.contentAsString
        assertThat(html)
            .contains(
                "<title>&lt;/script&gt;&lt;b&gt;Evil&lt;/b&gt; (CFR FRA000999888) — vessel photos | Open Vessel Photos</title>",
            ).contains("""<link rel="canonical" href="http://localhost/vessels/cfr/FRA000999888" />""")
            .contains(""""@type":"ImageObject"""")
            .contains(""""license":"https://creativecommons.org/licenses/by/4.0/"""")
            .contains("\\u003c/script>\\u003cb>Evil")
            .contains("<h1>&lt;/script&gt;&lt;b&gt;Evil&lt;/b&gt;</h1>")
            .doesNotContain("<b>Evil</b>")
            .doesNotContain("noindex")

        mockMvc.perform(get("/vessels/cfr/FRA000000001")).andExpect(status().isNotFound)
        mockMvc.perform(get("/vessels/mmsi/nope")).andExpect(status().isNotFound)
        mockMvc.perform(get("/unknown-page")).andExpect(status().isNotFound)
        assertThat(
            mockMvc
                .perform(get("/").param("q", "ker"))
                .andReturn()
                .response.contentAsString,
        ).contains("noindex")
        assertThat(
            mockMvc
                .perform(get("/"))
                .andReturn()
                .response.contentAsString,
        ).contains("""<a href="/vessels/cfr/FRA000999888">""")
            .doesNotContain("noindex")
        mockMvc.perform(get("/docs")).andExpect(status().isOk)

        mockMvc
            .perform(get("/sitemap.xml"))
            .andExpect(status().isOk)
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.containsString("<loc>http://localhost/sitemap-vessels-1.xml</loc>"),
                ),
            )
        mockMvc
            .perform(get("/sitemap-vessels-1.xml"))
            .andExpect(status().isOk)
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.containsString("<loc>http://localhost/vessels/cfr/FRA000999888</loc>"),
                ),
            ).andExpect(
                content().string(
                    org.hamcrest.Matchers.containsString(
                        "<image:loc>http://localhost/v1/cfr/FRA000999888/photos/1.jpg?size=full</image:loc>",
                    ),
                ),
            )
        mockMvc
            .perform(get("/robots.txt"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Sitemap: http://localhost/sitemap.xml")))
    }

    @Test
    fun `POST requests from outside the website Should need an active API key`() {
        fun upload(apiKey: String?) =
            mockMvc.perform(
                multipart("/v1/mmsi/227000777/photos")
                    .file(MockMultipartFile("file", "boat.jpg", "image/jpeg", TestImages.create(800, 600)))
                    .param("credit", "Script")
                    .param("licence", "CC0-1.0")
                    .apply { apiKey?.let { header("x-api-key", it) } },
            )

        upload(
            null,
        ).andExpect(
            status().isUnauthorized,
        ).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("/api-key")))
        upload("ovp_wrong").andExpect(status().isUnauthorized)

        // Request a key (open to everyone), then the admin approves it
        mockMvc
            .perform(
                post("/v1/api-key-requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"Fleet app","email":"dev@example.org","intendedUse":"Show vessel photos in our app"}""",
                    ),
            ).andExpect(status().isCreated)
        mockMvc
            .perform(
                post(
                    "/v1/api-key-requests",
                ).contentType(
                    MediaType.APPLICATION_JSON,
                ).content("""{"name":"x","email":"nope","intendedUse":"short"}"""),
            ).andExpect(status().isBadRequest)
        val cookie = adminCookie()
        val queue =
            mockMvc
                .perform(get("/v1/admin/queue").cookie(cookie))
                .andReturn()
                .response.contentAsString
        val requestId = Regex(""""apiKeyRequests":\[\{"id":(\d+)""").find(queue)!!.groupValues[1]
        val approval =
            mockMvc
                .perform(post("/v1/admin/api-keys/$requestId/approve").cookie(cookie))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.apiKey.status").value("ACTIVE"))
                .andReturn()
                .response.contentAsString
        val key = Regex(""""key":"(ovp_[A-Za-z0-9_-]+)"""").find(approval)!!.groupValues[1]

        upload(key).andExpect(status().isCreated)
        mockMvc
            .perform(get("/v1/admin/queue").cookie(cookie))
            .andExpect(jsonPath("$.photos[?(@.photo.submittedKey == '227000777')].apiKeyName").value("Fleet app"))
        mockMvc
            .perform(get("/v1/admin/api-keys").cookie(cookie))
            .andExpect(jsonPath("$[0].name").value("Fleet app"))
            .andExpect(jsonPath("$[0].lastUsedAt").isNotEmpty)

        mockMvc.perform(post("/v1/admin/api-keys/$requestId/revoke").cookie(cookie)).andExpect(status().isNoContent)
        upload(key).andExpect(status().isUnauthorized)

        // GET requests stay open
        mockMvc.perform(get("/v1/vessels/recent")).andExpect(status().isOk)
    }

    @Test
    fun `Imported vessels Should exist without photos, and be completed but never overwritten`() {
        val cookie = adminCookie()

        fun import(csv: String) =
            mockMvc
                .perform(
                    multipart("/v1/admin/vessels/import")
                        .file(MockMultipartFile("file", "fleet.csv", "text/csv", csv.toByteArray()))
                        .cookie(cookie),
                ).andExpect(status().isOk)

        // An export of the EU fleet register: extra columns, quoted fields, a vessel's events on several lines
        import(
            """
            Country Code;CFR;Event Code;Registration Nbr;Ext. Marking;Vessel Name;Port Code;Port Name
            FRA;FRA000752221;CST;752221;"LO 752221";;FRLRT;Lorient
            FRA;FRA000752221;MOD;752221;"LO 752221";LES MENHIRS;FRLRT;Lorient
            FRA;FRA000425329;CST;425329;LO 425329;MIREN;FRLRT;Lorient
            FRA;NOT-A-CFR;CST;1;X 1;BAD;;
            """.trimIndent(),
        ).andExpect(jsonPath("$.read").value(4))
            .andExpect(jsonPath("$.created").value(2))
            .andExpect(jsonPath("$.skipped").value(1))
            .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("Line 5")))

        mockMvc
            .perform(get("/v1/cfr/FRA000752221"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("LES MENHIRS"))
            .andExpect(jsonPath("$.registration").value("LO 752221"))
            .andExpect(jsonPath("$.port").value("Lorient"))
            .andExpect(jsonPath("$.photos").isEmpty)
        assertThat(
            mockMvc
                .perform(get("/vessels/cfr/FRA000752221"))
                .andReturn()
                .response.contentAsString,
        ).contains("<h1>LES MENHIRS</h1>")
            .contains("LO 752221, Lorient")
            .contains("No photo yet")
        mockMvc.perform(get("/v1/search").param("q", "lo752221")).andExpect(jsonPath("$[0].cfr").value("FRA000752221"))
        mockMvc.perform(get("/v1/search").param("q", "lorient")).andExpect(jsonPath("$.length()").value(2))
        assertThat(
            mockMvc
                .perform(get("/sitemap-vessels-1.xml"))
                .andReturn()
                .response.contentAsString,
        ).contains("<loc>http://localhost/vessels/cfr/FRA000752221</loc>")

        // A second import (simple format) fills in missing data only
        import(
            """
            cfr,mmsi,name,registration,port
            FRA000752221,,Another name,,
            FRA000425329,,MIREN,LO 425329,Lorient
            ,227000888,Sans CFR,,Étel
            """.trimIndent(),
        ).andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.updated").value(0))
            .andExpect(jsonPath("$.unchanged").value(2))
        mockMvc.perform(get("/v1/cfr/FRA000752221")).andExpect(jsonPath("$.name").value("LES MENHIRS"))
        mockMvc.perform(get("/v1/mmsi/227000888")).andExpect(jsonPath("$.port").value("Étel"))

        // Only the admin can import
        mockMvc
            .perform(
                multipart(
                    "/v1/admin/vessels/import",
                ).file(MockMultipartFile("file", "f.csv", "text/csv", "cfr\n".toByteArray())),
            ).andExpect(status().isUnauthorized)
    }
}
