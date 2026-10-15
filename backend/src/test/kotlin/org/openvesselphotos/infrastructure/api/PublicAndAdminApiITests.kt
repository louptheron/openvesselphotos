package org.openvesselphotos.infrastructure.api

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.openvesselphotos.TestImages
import org.openvesselphotos.config.AdminAuthentication
import org.openvesselphotos.domain.images.ImageDownloader
import org.openvesselphotos.domain.use_cases.public_api.RecordUsage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
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

    /** Imports would download from Wikimedia Commons */
    @MockitoBean
    lateinit var imageDownloader: ImageDownloader

    @Autowired
    lateinit var recordUsage: RecordUsage

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

        // An export of the EU fleet register: extra columns, quoted fields, a vessel's events on several lines, a vessel
        // that left the fleet
        import(
            """
            Country Code;CFR;Event Code;Registration Nbr;Ext. Marking;Vessel Name;Port Code;Port Name
            FRA;FRA000752221;CST;752221;"LO 752221";;FRLRT;Lorient
            FRA;FRA000752221;MOD;752221;"LO 752221";LES MENHIRS;FRLRT;Lorient
            FRA;FRA000425329;CST;425329;LO 425329;MIREN;FRLRT;Lorient
            FRA;NOT-A-CFR;CST;1;X 1;BAD;;
            FRA;FRA000111111;CST;111111;LO 111111;OLD BOAT;FRLRT;Lorient
            FRA;FRA000111111;RET;111111;LO 111111;OLD BOAT;FRLRT;Lorient
            """.trimIndent(),
        ).andExpect(jsonPath("$.read").value(6))
            .andExpect(jsonPath("$.created").value(2))
            .andExpect(jsonPath("$.outOfFleet").value(1))
            .andExpect(jsonPath("$.skipped").value(1))
            .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("Line 5")))
        mockMvc.perform(get("/v1/cfr/FRA000111111")).andExpect(status().isNotFound)

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
            .contains("noindex")
        mockMvc.perform(get("/v1/search").param("q", "lo752221")).andExpect(jsonPath("$[0].cfr").value("FRA000752221"))
        mockMvc.perform(get("/v1/search").param("q", "lorient")).andExpect(jsonPath("$.length()").value(2))
        assertThat(
            mockMvc
                .perform(get("/sitemap-vessels-1.xml"))
                .andReturn()
                .response.contentAsString,
        ).doesNotContain("<loc>http://localhost/vessels/cfr/FRA000752221</loc>")

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

    @Test
    fun `A photo sent with both keys Should give both keys to its vessel, unless they belong to another vessel`() {
        val cookie = adminCookie()

        fun send(
            path: String,
            vararg params: Pair<String, String>,
        ) = mockMvc.perform(
            multipart(path)
                .header("Sec-Fetch-Site", "same-origin")
                .file(MockMultipartFile("file", "boat.jpg", "image/jpeg", TestImages.create(1200, 800)))
                .param("credit", "J. Le Bihan")
                .param("licence", "CC-BY-4.0")
                .apply { params.forEach { (name, value) -> param(name, value) } },
        )

        val id =
            Regex("\"id\":(\\d+)")
                .find(
                    send("/v1/cfr/FRA000911288/photos", "mmsi" to "227000911", "vesselName" to "JULIEN COLEOU")
                        .andExpect(status().isCreated)
                        .andReturn()
                        .response.contentAsString,
                )!!
                .groupValues[1]
        mockMvc
            .perform(get("/v1/admin/queue").cookie(cookie))
            .andExpect(jsonPath("$.photos[?(@.photo.id == $id)].photo.submittedOtherKey").value("227000911"))
        mockMvc.perform(post("/v1/admin/photos/$id/approve").cookie(cookie)).andExpect(status().isNoContent)
        mockMvc
            .perform(get("/v1/mmsi/227000911"))
            .andExpect(jsonPath("$.cfr").value("FRA000911288"))
            .andExpect(jsonPath("$.name").value("JULIEN COLEOU"))

        // A mistyped MMSI is refused at once
        send("/v1/cfr/FRA000911288/photos", "mmsi" to "227000999").andExpect(status().isConflict)
        send("/v1/mmsi/227000911/photos", "cfr" to "FRA00091128").andExpect(status().isBadRequest)
    }

    @Test
    fun `Imported photos Should be published without review, once`() {
        val cookie = adminCookie()
        `when`(imageDownloader.download(anyString())).thenReturn(TestImages.create(1600, 1000))

        fun import(csv: String) =
            mockMvc
                .perform(
                    multipart("/v1/admin/photos/import")
                        .file(MockMultipartFile("file", "photos.csv", "text/csv", csv.toByteArray()))
                        .cookie(cookie),
                ).andExpect(status().isAccepted)

        fun waitForImport(): String {
            repeat(100) {
                val status =
                    mockMvc
                        .perform(get("/v1/admin/photos/import").cookie(cookie))
                        .andReturn()
                        .response.contentAsString
                if (status.contains("\"isRunning\":false")) {
                    return status
                }
                Thread.sleep(100)
            }
            throw AssertionError("The import did not finish.")
        }

        // A Commons export: extra columns, a quoted description over several lines, licences as labels or SPDX
        val csv =
            """
            mmsi,cfr,vessel_name,ovp_licence,licence,author,file_page,image_1600_url,date,title
            227000777,,Ar Men,,CC BY-SA 3.0,Jane Doe,https://commons.wikimedia.org/wiki/File:A.jpg,https://upload.wikimedia.org/a.jpg,2012-08-25 14:03:11,"Ar Men
            in Douarnenez"
            227000778,FRA000778778,Penn Ar Bed,CC-BY-4.0,CC BY 4.0,"Doe, John",https://commons.wikimedia.org/wiki/File:B.jpg,https://upload.wikimedia.org/b.jpg,,
            227000779,,No author,,CC0,,https://commons.wikimedia.org/wiki/File:C.jpg,https://upload.wikimedia.org/c.jpg,,
            """.trimIndent()
        import(csv)
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.invalid").value(1))
            .andExpect(jsonPath("$.errors[0]").value("Line 5: no author."))
        assertThat(waitForImport()).contains("\"imported\":2")

        mockMvc
            .perform(get("/v1/mmsi/227000777"))
            .andExpect(jsonPath("$.name").value("Ar Men"))
            .andExpect(jsonPath("$.photos[0].licence").value("CC-BY-SA-3.0"))
            .andExpect(jsonPath("$.photos[0].credit").value("Jane Doe"))
            .andExpect(jsonPath("$.photos[0].takenOn").value("2012-08-25"))
            .andExpect(jsonPath("$.photos[0].source").value("https://commons.wikimedia.org/wiki/File:A.jpg"))
        mockMvc
            .perform(get("/v1/cfr/FRA000778778"))
            .andExpect(jsonPath("$.mmsi").value("227000778"))
            .andExpect(jsonPath("$.photos[0].credit").value("Doe, John"))

        // Running the same file again (e.g. after a restart) does not duplicate the photos
        import(csv)
        assertThat(waitForImport()).contains("\"imported\":0").contains("\"alreadyImported\":2")
        mockMvc.perform(get("/v1/mmsi/227000777")).andExpect(jsonPath("$.photos.length()").value(1))

        // Only the admin can import
        mockMvc
            .perform(
                multipart(
                    "/v1/admin/photos/import",
                ).file(MockMultipartFile("file", "f.csv", "text/csv", csv.toByteArray())),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `Coverage Should count the register's vessels and those photographed`() {
        val cookie = adminCookie()

        fun coverage(field: String) =
            Regex(""""$field":(\d+)""")
                .find(
                    mockMvc
                        .perform(get("/v1/coverage"))
                        .andExpect(status().isOk)
                        .andReturn()
                        .response.contentAsString,
                )!!
                .groupValues[1]
                .toInt()

        val registerVessels = coverage("registerVessels")
        val registerPhotographed = coverage("registerPhotographed")
        val photos = coverage("photos")

        mockMvc
            .perform(
                multipart("/v1/admin/vessels/import")
                    .file(
                        MockMultipartFile(
                            "file",
                            "fleet.csv",
                            "text/csv",
                            "cfr;name\nFRA000900001;COVERED\nFRA000900002;NOT YET".toByteArray(),
                        ),
                    ).cookie(cookie),
            ).andExpect(status().isOk)
        submitPhoto("cfr", "FRA000900001")
        val queue =
            mockMvc
                .perform(get("/v1/admin/queue").cookie(cookie))
                .andReturn()
                .response.contentAsString
        val photoId =
            Regex(""""id":(\d+),"number":null,"submittedKeyType":"cfr","submittedKey":"FRA000900001"""")
                .find(queue)!!
                .groupValues[1]
        mockMvc.perform(post("/v1/admin/photos/$photoId/approve").cookie(cookie)).andExpect(status().isNoContent)

        assertThat(coverage("registerVessels")).isEqualTo(registerVessels + 2)
        assertThat(coverage("registerPhotographed")).isEqualTo(registerPhotographed + 1)
        assertThat(coverage("photos")).isEqualTo(photos + 1)
        assertThat(coverage("photographedVessels")).isGreaterThanOrEqualTo(coverage("registerPhotographed"))
    }

    @Test
    fun `Ports Should list their vessels and how many are photographed`() {
        val cookie = adminCookie()
        mockMvc
            .perform(
                multipart("/v1/admin/vessels/import")
                    .file(
                        MockMultipartFile(
                            "file",
                            "fleet.csv",
                            "text/csv",
                            (
                                "CFR;Name of Vessel;Place of Registration Code;" +
                                    "Place of Registration Name;Event Type\n" +
                                    "FRA000800001;ALPHA;FRM2R;Marennes;MOD\n" +
                                    "FRA000800002;BRAVO;FRM2R;Marennes;CST\n" +
                                    "FRA000800003;CHARLIE;MQFDF;Fort-De-France;MOD\n"
                            ).toByteArray(),
                        ),
                    ).cookie(cookie),
            ).andExpect(status().isOk)
        submitPhoto("cfr", "FRA000800001")
        val queue =
            mockMvc
                .perform(get("/v1/admin/queue").cookie(cookie))
                .andReturn()
                .response.contentAsString
        val photoId =
            Regex(""""id":(\d+),"number":null,"submittedKeyType":"cfr","submittedKey":"FRA000800001"""")
                .find(queue)!!
                .groupValues[1]
        mockMvc.perform(post("/v1/admin/photos/$photoId/approve").cookie(cookie)).andExpect(status().isNoContent)

        mockMvc
            .perform(get("/v1/ports"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code == 'FRM2R')].vessels").value(org.hamcrest.Matchers.contains(2)))
            .andExpect(jsonPath("$[?(@.code == 'FRM2R')].photographed").value(org.hamcrest.Matchers.contains(1)))
            .andExpect(
                jsonPath("$[?(@.code == 'MQFDF')].name").value(org.hamcrest.Matchers.contains("Fort-de-France")),
            ).andExpect(jsonPath("$[?(@.code == 'MQFDF')].country").value(org.hamcrest.Matchers.contains("MQ")))
        mockMvc
            .perform(get("/v1/ports/frm2r"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Marennes"))
            .andExpect(jsonPath("$.latitude").isNumber)
        mockMvc
            .perform(get("/v1/ports/FRM2R/vessels").param("photographed", "true"))
            .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("ALPHA")))
            .andExpect(jsonPath("$[0].portCode").value("FRM2R"))
            .andExpect(jsonPath("$[0].photoCount").value(1))
        mockMvc
            .perform(get("/v1/ports/FRM2R/vessels").param("photographed", "false"))
            .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("BRAVO")))
        mockMvc.perform(get("/v1/ports/ZZZZZ")).andExpect(status().isNotFound)

        assertThat(
            mockMvc
                .perform(get("/ports/FRM2R"))
                .andExpect(status().isOk)
                .andReturn()
                .response.contentAsString,
        ).contains("Vessels of Marennes (FRM2R)", "ALPHA", "BRAVO")
        mockMvc.perform(get("/ports/ZZZZZ")).andExpect(status().isNotFound)
        assertThat(
            mockMvc
                .perform(get("/sitemap-pages.xml"))
                .andReturn()
                .response.contentAsString,
        ).contains("/ports/FRM2R")
    }

    @Test
    fun `Register data Should be imported, then replaced by a newer export without overwriting the name`() {
        val cookie = adminCookie()

        fun import(
            name: String,
            length: String,
        ) = mockMvc
            .perform(
                multipart("/v1/admin/vessels/import")
                    .file(
                        MockMultipartFile(
                            "file",
                            "fleet.csv",
                            "text/csv",
                            (
                                "CFR;Name of Vessel;Vessel Type;Year of Construction;LOA (m);" +
                                    "Power of Main Engine (kW);Tonnage GT;Main Fishing Gear;" +
                                    "Subsidiary Fishing Gear 1;Subsidiary Fishing Gear 2;IRCS\n" +
                                    "FRA000700001;$name;TO;1998;$length;331.00;139.46;OTB;NO;GNS;FHZV\n"
                            ).toByteArray(),
                        ),
                    ).cookie(cookie),
            ).andExpect(status().isOk)

        import("LES MENHIRS", "22.00").andExpect(jsonPath("$.created").value(1))
        mockMvc
            .perform(get("/v1/cfr/FRA000700001"))
            .andExpect(jsonPath("$.register.vesselType").value("TO"))
            .andExpect(jsonPath("$.register.yearBuilt").value(1998))
            .andExpect(jsonPath("$.register.lengthMeters").value(22.0))
            .andExpect(jsonPath("$.register.enginePowerKw").value(331.0))
            .andExpect(jsonPath("$.register.tonnageGt").value(139.46))
            .andExpect(jsonPath("$.register.mainGear").value("OTB"))
            .andExpect(jsonPath("$.register.otherGears[*]").value(org.hamcrest.Matchers.contains("GNS")))
            .andExpect(jsonPath("$.register.callSign").value("FHZV"))

        // Same data: nothing to update
        import("LES MENHIRS", "22.00").andExpect(jsonPath("$.updated").value(0))
        // Lengthened, and renamed in the file: the length is replaced, the name kept
        import("OTHER NAME", "24.50").andExpect(jsonPath("$.updated").value(1))
        mockMvc
            .perform(get("/v1/cfr/FRA000700001"))
            .andExpect(jsonPath("$.name").value("LES MENHIRS"))
            .andExpect(jsonPath("$.register.lengthMeters").value(24.5))
        assertThat(
            mockMvc
                .perform(get("/vessels/cfr/FRA000700001"))
                .andReturn()
                .response.contentAsString,
        ).contains("Built in 1998 · 24.5 m · 331 kW · 139.46 GT · main gear OTB · call sign FHZV")
    }

    @Test
    fun `Usage statistics Should count images by the site showing them, and API calls by key`() {
        val cookie = adminCookie()
        val id =
            Regex("\"id\":(\\d+)")
                .find(
                    mockMvc
                        .perform(
                            multipart("/v1/mmsi/227000555/photos")
                                .header("Sec-Fetch-Site", "same-origin")
                                .file(MockMultipartFile("file", "boat.jpg", "image/jpeg", TestImages.create(800, 600)))
                                .param("credit", "J. Le Bihan")
                                .param("licence", "CC-BY-4.0"),
                        ).andReturn()
                        .response.contentAsString,
                )!!
                .groupValues[1]
        mockMvc.perform(post("/v1/admin/photos/$id/approve").cookie(cookie)).andExpect(status().isNoContent)
        mockMvc.perform(
            post("/v1/api-key-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"name":"Stats app","email":"stats@example.org","intendedUse":"Count the calls of this key"}""",
                ),
        )
        val requestId =
            Regex(""""id":(\d+),"name":"Stats app"""")
                .find(
                    mockMvc
                        .perform(get("/v1/admin/queue").cookie(cookie))
                        .andReturn()
                        .response.contentAsString,
                )!!
                .groupValues[1]
        val key =
            Regex(""""key":"(ovp_[A-Za-z0-9_-]+)"""")
                .find(
                    mockMvc
                        .perform(post("/v1/admin/api-keys/$requestId/approve").cookie(cookie))
                        .andReturn()
                        .response.contentAsString,
                )!!
                .groupValues[1]

        // An <img> on another site (twice), on this site, a call with the key, one with an invalid key, a 404
        repeat(2) {
            mockMvc
                .perform(get("/v1/mmsi/227000555.jpg").header("Referer", "https://www.stats-test.example/fleet"))
                .andExpect(status().isOk)
        }
        mockMvc.perform(get("/v1/mmsi/227000555.jpg").header("Referer", "http://localhost/vessels/mmsi/227000555"))
        mockMvc.perform(get("/v1/mmsi/227000555").header("x-api-key", key)).andExpect(status().isOk)
        mockMvc.perform(get("/v1/mmsi/227000555").header("x-api-key", "ovp_wrong")).andExpect(status().isOk)
        mockMvc.perform(get("/v1/mmsi/227000000.jpg").header("Referer", "https://not-counted.example/"))
        recordUsage.flush()

        mockMvc
            .perform(get("/v1/admin/usage").cookie(cookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.days").value(30))
            .andExpect(jsonPath("$.daily.length()").value(30))
            // At least the vessel of this test's photo, created today
            .andExpect(jsonPath("$.daily[29].vesselsAdded").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.vesselsAdded").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.imageSources[?(@.source == 'stats-test.example')].count").value(2))
            .andExpect(jsonPath("$.imageSources[?(@.source == 'not-counted.example')]").isEmpty)
            .andExpect(jsonPath("$.apiKeys[?(@.name == 'Stats app')].calls").value(1))
        assertThat(
            mockMvc
                .perform(get("/v1/admin/usage").cookie(cookie))
                .andReturn()
                .response.contentAsString,
        ).contains("\"source\":\"website\"")

        mockMvc.perform(get("/v1/admin/usage")).andExpect(status().isUnauthorized)
    }
}
