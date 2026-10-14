package org.openvesselphotos.infrastructure.api.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RateLimitFilterUTests {
    private val filter =
        RateLimitFilter(
            clientIp = ClientIp("", false),
            readsPerMinute = 3,
            writesPerHour = 1,
            apiKeyWritesPerHour = 2,
            apiKeyRequestsPerHour = 1,
        )

    private fun send(
        method: String,
        path: String,
        ip: String = "1.2.3.4",
        apiKeyId: Int? = null,
    ): MockHttpServletResponse {
        val request =
            MockHttpServletRequest(method, path).apply {
                remoteAddr = ip
                apiKeyId?.let { setAttribute(ApiKeyCheckFilter.API_KEY_ID_ATTRIBUTE, it) }
            }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())

        return response
    }

    @Test
    fun `Reads Should be limited per IP`() {
        val statuses = (1..4).map { send("GET", "/v1/cfr/FRA000123456.jpg").status }

        assertThat(statuses).containsExactly(200, 200, 200, 429)
        assertThat(send("GET", "/v1/search", ip = "5.6.7.8").status).isEqualTo(200)
    }

    @Test
    fun `Writes Should be limited per IP without key, and per API key with one`() {
        assertThat(send("POST", "/v1/mmsi/227000101/photos").status).isEqualTo(200)
        val limited = send("POST", "/v1/mmsi/227000101/link")
        assertThat(limited.status).isEqualTo(429)
        assertThat(limited.getHeader("Retry-After")).isNotNull()
        assertThat(limited.contentAsString).contains("TOO_MANY_REQUESTS")

        // The same IP with an API key has the key's limit
        assertThat(
            (1..3).map { send("POST", "/v1/mmsi/227000101/photos", apiKeyId = 7).status },
        ).containsExactly(200, 200, 429)
    }

    @Test
    fun `API key requests Should have their own limit, and the admin none`() {
        assertThat(send("POST", "/v1/api-key-requests").status).isEqualTo(200)
        assertThat(send("POST", "/v1/api-key-requests").status).isEqualTo(429)
        assertThat((1..5).map { send("GET", "/v1/admin/queue").status }).containsOnly(200)
    }
}
