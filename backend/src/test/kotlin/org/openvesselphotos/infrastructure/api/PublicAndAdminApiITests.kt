package org.openvesselphotos.infrastructure.api

import jakarta.servlet.http.Cookie
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
            registry.add("openvesselphotos.database-url") {
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
                .andExpect(jsonPath("$.photos[0].photo.submittedKey").value("FRA000123456"))
                .andExpect(jsonPath("$.photos[0].vessel").doesNotExist())
                .andReturn()
                .response.contentAsString
        val photoId = Regex(""""id":(\d+)""").find(queue)!!.groupValues[1]
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
        mockMvc
            .perform(get("/v1/search").param("q", "ker"))
            .andExpect(jsonPath("$[0].mmsi").value("227000101"))
            .andExpect(jsonPath("$[0].mainPhoto.urls.mmsi").value("http://localhost/v1/mmsi/227000101/photos/1.jpg"))
        mockMvc.perform(get("/v1/vessels/recent")).andExpect(jsonPath("$[0].cfr").value("FRA000123456"))

        // A report removes the photo
        mockMvc
            .perform(
                post("/v1/mmsi/227000101/photos/1/reports")
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
    fun `The React app Should be served on its routes`() {
        mockMvc.perform(get("/vessels/cfr/FRA000123456")).andExpect(status().isOk)
    }
}
