package org.openvesselphotos.infrastructure.usage

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.openvesselphotos.domain.entities.UsageKey
import org.openvesselphotos.domain.entities.UsageKind
import org.openvesselphotos.domain.use_cases.public_api.RecordUsage
import org.openvesselphotos.infrastructure.api.security.ApiKeyCheckFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URI

/**
 * Counts the successful requests to the public API, after [ApiKeyCheckFilter] has identified the API key. Images are
 * counted by the site showing them, read from the Referer header: browsers send the origin of the page of an `<img>`
 * (e.g. "https://monitorfish.fr/"). Images are cached for a day, so a browser showing a photo again is not counted.
 */
@Component
@Order(2)
class UsageCountingFilter(
    private val recordUsage: RecordUsage,
    @param:Value("\${openvesselphotos.public-url}")
    publicUrl: String,
) : OncePerRequestFilter() {
    private val publicHost = publicUrl.takeIf { it.isNotBlank() }?.let { hostOf(it) }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)

        return !path.startsWith("/v1/") || path.startsWith("/v1/admin/") || request.method == "OPTIONS"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        filterChain.doFilter(request, response)

        if (response.status >= 400) {
            return
        }
        val path = request.requestURI.removePrefix(request.contextPath)
        val kind = if (request.method == "GET" && IMAGE_PATH.matches(path)) UsageKind.IMAGE else UsageKind.API
        recordUsage.record(
            kind,
            sourceOf(request),
            request.getAttribute(ApiKeyCheckFilter.API_KEY_ID_ATTRIBUTE) as Int?,
        )
    }

    private fun sourceOf(request: HttpServletRequest): String {
        val host = request.getHeader("Referer")?.let { hostOf(it) } ?: return UsageKey.DIRECT

        return if (host == hostOf("http://${request.serverName}") || host == publicHost) UsageKey.WEBSITE else host
    }

    /** The host without "www.", e.g. "monitorfish.fr", or null for an invalid URL. */
    private fun hostOf(url: String): String? =
        runCatching { URI(url.trim()).host }
            .getOrNull()
            ?.lowercase()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_HOST_LENGTH }

    companion object {
        /** /v1/{cfr|mmsi}/{key}.jpg, /v1/{cfr|mmsi}/{key}/photos/{n}.jpg and /v1/{cfr|mmsi}/{key}/photos/{n}/{name}.jpg */
        private val IMAGE_PATH = Regex("^/v1/(cfr|mmsi)/[A-Za-z0-9]+(/photos/[0-9]+(/[a-z0-9-]+)?)?\\.jpg$")
        private const val MAX_HOST_LENGTH = 255
    }
}
