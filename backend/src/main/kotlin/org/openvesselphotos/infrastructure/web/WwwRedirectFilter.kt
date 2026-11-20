package org.openvesselphotos.infrastructure.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Redirects the www. host of each site to its public URL (OPENVESSELPHOTOS_PUBLIC_URL,
 * OPENVESSELPHOTOS_PORTS_PUBLIC_URL), e.g. www.openvesselphotos.org to openvesselphotos.org: otherwise search engines
 * crawl each page twice. Other hosts (e.g. *.scalingo.io) are served.
 */
@Component
@Order(0)
class WwwRedirectFilter(
    publicUrl: PublicUrl,
) : OncePerRequestFilter() {
    /** e.g. "www.openvesselphotos.org" to "https://openvesselphotos.org" */
    private val redirects =
        Site.entries
            .map { publicUrl.configured(it) }
            .filter { it.isNotEmpty() }
            .mapNotNull { url -> PublicUrl.hostOf(url)?.let { "www.$it" to url } }
            .toMap()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.serverName.lowercase() !in redirects

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val publicUrl = redirects.getValue(request.serverName.lowercase())
        response.status = HttpServletResponse.SC_MOVED_PERMANENTLY
        response.setHeader("Location", publicUrl + request.requestURI + (request.queryString?.let { "?$it" } ?: ""))
    }
}
