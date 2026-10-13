package org.openvesselphotos.infrastructure.api.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class ClientIpUTests {
    private val request =
        MockHttpServletRequest().apply {
            remoteAddr = "10.0.0.1"
            addHeader("X-Real-IP", "203.0.113.7")
        }

    @Test
    fun `of Should use the configured proxy header`() {
        assertThat(ClientIp("X-Real-IP").of(request)).isEqualTo("203.0.113.7")
    }

    @Test
    fun `of Should ignore headers When none is configured, as clients could forge them`() {
        assertThat(ClientIp("").of(request)).isEqualTo("10.0.0.1")
    }
}
