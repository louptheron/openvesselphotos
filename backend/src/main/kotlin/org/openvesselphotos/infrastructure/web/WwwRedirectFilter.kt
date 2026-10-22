package org.openvesselphotos.infrastructure.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URI

/**
 * Redirects the www. host to the public URL (OPENVESSELPHOTOS_PUBLIC_URL), e.g. www.openvesselphotos.org to
 * openvesselphotos.org: otherwise search engines crawl each page twice. Other hosts (e.g. *.scalingo.io) are served.
 */
@Component
@Order(0)
class WwwRedirectFilter(
    @param:Value("\${openvesselphotos.public-url}")
    private val configuredUrl: String,
) : OncePerRequestFilter() {
    private val publicUrl = configuredUrl.trimEnd('/')
    private val wwwHost = publicUrl.takeIf { it.isNotEmpty() }?.let { "www.${URI.create(it).host}" }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        wwwHost == null || !request.serverName.equals(wwwHost, ignoreCase = true)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        response.status = HttpServletResponse.SC_MOVED_PERMANENTLY
        response.setHeader("Location", publicUrl + request.requestURI + (request.queryString?.let { "?$it" } ?: ""))
    }
}
