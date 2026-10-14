package org.openvesselphotos.infrastructure.api.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class ClientIpUTests {
    private fun request(
        realIp: String,
        cloudflareIp: String? = null,
    ) = MockHttpServletRequest().apply {
        remoteAddr = "10.0.0.1"
        addHeader("X-Real-IP", realIp)
        cloudflareIp?.let { addHeader("CF-Connecting-IP", it) }
    }

    @Test
    fun `of Should use the configured proxy header`() {
        assertThat(ClientIp("X-Real-IP", false).of(request("203.0.113.7"))).isEqualTo("203.0.113.7")
    }

    @Test
    fun `of Should ignore headers When none is configured, as clients could forge them`() {
        assertThat(ClientIp("", false).of(request("203.0.113.7"))).isEqualTo("10.0.0.1")
    }

    @Test
    fun `of Should use CF-Connecting-IP When behind Cloudflare and the request comes from Cloudflare`() {
        val clientIp = ClientIp("X-Real-IP", true)

        assertThat(clientIp.of(request("162.158.1.2", "203.0.113.7"))).isEqualTo("203.0.113.7")
        assertThat(clientIp.of(request("2606:4700::1", "2001:db8::7"))).isEqualTo("2001:db8::7")
    }

    @Test
    fun `of Should ignore CF-Connecting-IP When the request doesn't come from Cloudflare, as it could be forged`() {
        // Sent to the *.scalingo.io domain directly, bypassing Cloudflare
        assertThat(ClientIp("X-Real-IP", true).of(request("203.0.113.66", "198.51.100.1"))).isEqualTo("203.0.113.66")
        // Not behind Cloudflare
        assertThat(ClientIp("X-Real-IP", false).of(request("162.158.1.2", "198.51.100.1"))).isEqualTo("162.158.1.2")
    }

    @Test
    fun `isCloudflare Should match Cloudflare's ranges only, without resolving host names`() {
        assertThat(ClientIp.isCloudflare("104.16.0.1")).isTrue()
        assertThat(ClientIp.isCloudflare("104.23.255.255")).isTrue()
        assertThat(ClientIp.isCloudflare("104.24.0.0")).isTrue()
        assertThat(ClientIp.isCloudflare("104.15.255.255")).isFalse()
        assertThat(ClientIp.isCloudflare("2a06:98c7:ffff::1")).isTrue()
        assertThat(ClientIp.isCloudflare("2a06:98c8::1")).isFalse()
        assertThat(ClientIp.isCloudflare("cloudflare.com")).isFalse()
        assertThat(ClientIp.isCloudflare("")).isFalse()
    }
}
