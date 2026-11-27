package org.openvesselphotos.infrastructure.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class SiteHostFilterUTests {
    private val publicUrl = PublicUrl("https://openvesselphotos.org", "https://ports-de-peche.fr")

    private fun send(
        host: String,
        path: String,
    ): MockHttpServletResponse {
        val request = MockHttpServletRequest("GET", path).apply { serverName = host }
        val response = MockHttpServletResponse()
        SiteHostFilter(publicUrl).doFilter(request, response, MockFilterChain())

        return response
    }

    @Test
    fun `siteOf Should tell the ports site by its host, with or without www, and in local development`() {
        assertThat(publicUrl.siteOf("ports-de-peche.fr")).isEqualTo(Site.PORTS)
        assertThat(publicUrl.siteOf("WWW.ports-de-peche.fr")).isEqualTo(Site.PORTS)
        assertThat(publicUrl.siteOf("ports.localhost")).isEqualTo(Site.PORTS)
        assertThat(publicUrl.siteOf("openvesselphotos.org")).isEqualTo(Site.OPENVESSELPHOTOS)
        assertThat(publicUrl.siteOf("openvesselphotos.osc-fr1.scalingo.io")).isEqualTo(Site.OPENVESSELPHOTOS)
        assertThat(PublicUrl("", "").siteOf("ports-de-peche.fr")).isEqualTo(Site.OPENVESSELPHOTOS)
    }

    @Test
    fun `The pages of the ports host Should be forwarded to the ports pages`() {
        assertThat(send("ports-de-peche.fr", "/").forwardedUrl).isEqualTo("/_ports/")
        assertThat(send("ports-de-peche.fr", "/FRQUY").forwardedUrl).isEqualTo("/_ports/FRQUY")
        assertThat(send("ports-de-peche.fr", "/robots.txt").forwardedUrl).isEqualTo("/_ports/robots.txt")
        assertThat(send("ports-de-peche.fr", "/ports/erquy").forwardedUrl).isEqualTo("/_ports/ports/erquy")
        assertThat(send("ports-de-peche.fr", "/llms.txt").forwardedUrl).isEqualTo("/_ports/llms.txt")
        assertThat(send("ports-de-peche.fr", "/donnees/ports.csv").forwardedUrl).isEqualTo("/_ports/donnees/ports.csv")
        // Pages of Open Vessel Photos are not found there
        assertThat(send("ports-de-peche.fr", "/vessels/cfr/FRA000123456").forwardedUrl)
            .isEqualTo("/_ports/vessels/cfr/FRA000123456")
    }

    @Test
    fun `The API, assets and favicon Should be served on both hosts`() {
        listOf(
            "/v1/ports",
            "/assets/index-abc.js",
            "/favicon.svg",
            "/ports-favicon.ico",
            "/ports-favicon-96.png",
        ).forEach {
            val response = send("ports-de-peche.fr", it)

            assertThat(response.forwardedUrl).isNull()
            assertThat(response.status).isEqualTo(200)
        }
    }

    @Test
    fun `The root icons Should be the ports site's own on its host`() {
        assertThat(send("ports-de-peche.fr", "/favicon.ico").forwardedUrl).isEqualTo("/ports-favicon.ico")
        assertThat(send("ports-de-peche.fr", "/apple-touch-icon.png").forwardedUrl)
            .isEqualTo("/ports-apple-touch-icon.png")
        assertThat(send("openvesselphotos.org", "/favicon.ico").forwardedUrl).isNull()
    }

    @Test
    fun `The ports pages Should not be found on the Open Vessel Photos host, nor called directly`() {
        assertThat(send("openvesselphotos.org", "/_ports/FRQUY").status).isEqualTo(404)
        assertThat(send("openvesselphotos.org", "/ports.html").status).isEqualTo(404)
        assertThat(send("ports-de-peche.fr", "/_ports/FRQUY").status).isEqualTo(404)
        assertThat(send("openvesselphotos.org", "/vessels").forwardedUrl).isNull()
        assertThat(send("openvesselphotos.org", "/vessels").status).isEqualTo(200)
    }
}
