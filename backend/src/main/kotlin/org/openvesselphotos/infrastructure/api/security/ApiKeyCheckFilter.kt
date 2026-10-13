package org.openvesselphotos.infrastructure.api.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.openvesselphotos.domain.use_cases.public_api.AuthenticateApiKey
import org.openvesselphotos.infrastructure.web.PublicUrl
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * POST requests to the public API need an API key in the `x-api-key` header, as in MonitorFish.
 *
 * Requests from this website's own forms (contribute, report, suggest a key) are sent by the browser from the same
 * origin and pass without a key: a key cannot be hidden in the frontend code.
 */
@Component
class ApiKeyCheckFilter(
    private val authenticateApiKey: AuthenticateApiKey,
    private val publicUrl: PublicUrl,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)

        return request.method != "POST" ||
            !path.startsWith("/v1/") ||
            path.startsWith("/v1/admin/") ||
            path == API_KEY_REQUESTS_PATH
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val key = request.getHeader(API_KEY_HEADER)

        if (key.isNullOrBlank()) {
            if (isFromThisWebsite(request)) {
                filterChain.doFilter(request, response)
            } else {
                unauthorized(
                    response,
                    "An API key is required (x-api-key header): request one at ${publicUrl.get()}/api-key",
                )
            }

            return
        }

        val apiKey = authenticateApiKey.execute(key)
        if (apiKey == null) {
            logger.warn("Invalid or revoked API key used from IP: ${request.remoteAddr}.")
            unauthorized(response, "Invalid or revoked API key.")

            return
        }

        request.setAttribute(API_KEY_ID_ATTRIBUTE, apiKey.id)
        filterChain.doFilter(request, response)
    }

    /** Browsers send `Sec-Fetch-Site` (and `Origin` on POST), which a web page cannot change. */
    private fun isFromThisWebsite(request: HttpServletRequest): Boolean {
        if (request.getHeader("Sec-Fetch-Site") == "same-origin") {
            return true
        }
        val origin = request.getHeader("Origin") ?: return false

        return origin == ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() ||
            origin == publicUrl.get()
    }

    private fun unauthorized(
        response: HttpServletResponse,
        message: String,
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write("""{"code":"UNAUTHORIZED","message":"${message.replace("\"", "'")}"}""")
    }

    companion object {
        const val API_KEY_HEADER = "x-api-key"
        const val API_KEY_ID_ATTRIBUTE = "openvesselphotos.apiKeyId"
        const val API_KEY_REQUESTS_PATH = "/v1/api-key-requests"
    }
}
