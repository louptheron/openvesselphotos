package org.openvesselphotos.infrastructure.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class WwwRedirectFilterUTests {
    private fun send(
        publicUrl: String,
        host: String,
        path: String,
        query: String? = null,
    ): MockHttpServletResponse {
        val request =
            MockHttpServletRequest("GET", path).apply {
                serverName = host
                queryString = query
            }
        val response = MockHttpServletResponse()
        WwwRedirectFilter(publicUrl).doFilter(request, response, MockFilterChain())

        return response
    }

    @Test
    fun `The www host Should be redirected to the public URL, with its path and query`() {
        val response = send("https://openvesselphotos.org/", "www.openvesselphotos.org", "/vessels", "page=2")

        assertThat(response.status).isEqualTo(301)
        assertThat(response.getHeader("Location")).isEqualTo("https://openvesselphotos.org/vessels?page=2")
    }

    @Test
    fun `Other hosts Should be served`() {
        assertThat(send("https://openvesselphotos.org", "openvesselphotos.org", "/").status).isEqualTo(200)
        assertThat(send("https://openvesselphotos.org", "openvesselphotos.osc-fr1.scalingo.io", "/").status)
            .isEqualTo(200)
        // Without a public URL, every host is served
        assertThat(send("", "www.openvesselphotos.org", "/").status).isEqualTo(200)
    }
}
