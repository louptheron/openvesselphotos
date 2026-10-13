package org.openvesselphotos.infrastructure.api.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

/**
 * Limits the requests to the public API, per client IP, or per API key for requests sent with one.
 * Runs after [ApiKeyCheckFilter], which identifies the API key.
 */
@Component
@Order(2)
class RateLimitFilter(
    private val clientIp: ClientIp,
    @param:Value("\${openvesselphotos.rate-limits.reads-per-minute}")
    private val readsPerMinute: Int,
    @param:Value("\${openvesselphotos.rate-limits.writes-per-hour}")
    private val writesPerHour: Int,
    @param:Value("\${openvesselphotos.rate-limits.api-key-writes-per-hour}")
    private val apiKeyWritesPerHour: Int,
    @param:Value("\${openvesselphotos.rate-limits.api-key-requests-per-hour}")
    private val apiKeyRequestsPerHour: Int,
) : OncePerRequestFilter() {
    private val rateLimiter = RateLimiter()

    private data class Rule(
        val name: String,
        val limit: Int,
        val window: Duration,
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)

        // The admin login has its own lockout; the rest of the admin needs a session
        return !path.startsWith("/v1/") || path.startsWith("/v1/admin/") || request.method == "OPTIONS"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = request.requestURI.removePrefix(request.contextPath)
        val apiKeyId = request.getAttribute(ApiKeyCheckFilter.API_KEY_ID_ATTRIBUTE) as Int?
        val (rule, client) =
            when {
                request.method == "GET" || request.method == "HEAD" ->
                    Rule("read", readsPerMinute, Duration.ofMinutes(1)) to clientIp.of(request)
                path == ApiKeyCheckFilter.API_KEY_REQUESTS_PATH ->
                    Rule("api-key-request", apiKeyRequestsPerHour, Duration.ofHours(1)) to clientIp.of(request)
                apiKeyId != null ->
                    Rule("api-key-write", apiKeyWritesPerHour, Duration.ofHours(1)) to "key:$apiKeyId"
                else ->
                    Rule("write", writesPerHour, Duration.ofHours(1)) to clientIp.of(request)
            }

        val decision = rateLimiter.tryConsume("${rule.name}:$client", rule.limit, rule.window)
        response.setHeader("X-RateLimit-Limit", decision.limit.toString())
        response.setHeader("X-RateLimit-Remaining", decision.remaining.toString())
        response.setHeader("X-RateLimit-Reset", decision.resetInSeconds.toString())

        if (!decision.isAllowed) {
            logger.warn("Rate limit '${rule.name}' exceeded by $client.")
            response.status = 429
            response.setHeader("Retry-After", decision.resetInSeconds.toString())
            response.contentType = "application/json"
            response.characterEncoding = "UTF-8"
            response.writer.write(
                """{"code":"TOO_MANY_REQUESTS","message":"Too many requests, retry in ${decision.resetInSeconds} seconds."}""",
            )

            return
        }

        filterChain.doFilter(request, response)
    }
}
