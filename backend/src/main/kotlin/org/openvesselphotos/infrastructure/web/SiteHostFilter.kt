package org.openvesselphotos.infrastructure.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Serves the ports site on its own host: its pages are forwarded to [PORTS_PREFIX] (e.g. "/FRQUY" to "/_ports/FRQUY",
 * see [PortsPagesController]), while the shared API, assets and favicons are served as they are. So no page is served
 * on both hosts, which search engines would see as duplicate content: the ports pages are not found on the Open Vessel
 * Photos host, nor its pages on the ports host.
 */
@Component
@Order(1)
class SiteHostFilter(
    private val publicUrl: PublicUrl,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = request.requestURI.removePrefix(request.contextPath)
        val isPortsPath = path == PORTS_PREFIX || path.startsWith("$PORTS_PREFIX/") || path == PORTS_TEMPLATE_PATH

        when {
            publicUrl.siteOf(request.serverName) == Site.OPENVESSELPHOTOS ->
                if (isPortsPath) {
                    response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                    )
                } else {
                    filterChain.doFilter(request, response)
                }
            isPortsPath -> response.sendError(HttpServletResponse.SC_NOT_FOUND)
            SHARED_PATHS.any { path.startsWith(it) } || path in SHARED_FILES -> filterChain.doFilter(request, response)
            else -> request.getRequestDispatcher(PORTS_PREFIX + path).forward(request, response)
        }
    }

    companion object {
        const val PORTS_PREFIX = "/_ports"

        /** The raw template of the ports pages, see [Site.PORTS] */
        private const val PORTS_TEMPLATE_PATH = "/ports.html"
        private val SHARED_PATHS = listOf("/v1/", "/assets/")
        private val SHARED_FILES = setOf("/favicon.svg", "/ports-favicon.svg")
    }
}
