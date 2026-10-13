package org.openvesselphotos.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Admin access with a unique password (ADMIN_PASSWORD). A successful login sets a signed session cookie;
 * the signing key derives from the password, so changing the password signs everybody out.
 */
@Component
class AdminAuthentication(
    @param:Value("\${openvesselphotos.admin-password}")
    private val adminPassword: String,
) {
    private val logger = LoggerFactory.getLogger(AdminAuthentication::class.java)
    private val failedAttempts = ConcurrentHashMap<String, Pair<Int, Instant>>()
    private val signingKey = sha256("openvesselphotos-admin-session:$adminPassword")

    init {
        if (!isEnabled()) {
            logger.warn("ADMIN_PASSWORD is not set: the admin is disabled.")
        }
    }

    fun isEnabled() = adminPassword.isNotBlank()

    /** Returns the session cookie for a correct password. */
    fun login(
        password: String,
        clientIp: String,
        secure: Boolean,
    ): ResponseCookie {
        val now = Instant.now()
        val (count, since) = failedAttempts[clientIp] ?: (0 to now)
        if (count >= MAX_FAILED_ATTEMPTS && since.plus(LOCK_DURATION).isAfter(now)) {
            throw BackendUsageException(
                BackendUsageErrorCode.TOO_MANY_REQUESTS,
                "Too many failed attempts, try again later.",
            )
        }

        if (!isEnabled() || !MessageDigest.isEqual(sha256(password), sha256(adminPassword))) {
            val isWindowOver = since.plus(LOCK_DURATION).isBefore(now)
            failedAttempts[clientIp] = if (isWindowOver) 1 to now else count + 1 to since
            throw BackendUsageException(BackendUsageErrorCode.UNAUTHORIZED, "Wrong password.")
        }
        failedAttempts.remove(clientIp)

        val expiresAt = now.plus(SESSION_DURATION).epochSecond.toString()
        return cookie("$expiresAt.${sign(expiresAt)}", SESSION_DURATION, secure)
    }

    fun logoutCookie(secure: Boolean): ResponseCookie = cookie("", Duration.ZERO, secure)

    fun isAuthenticated(request: HttpServletRequest): Boolean {
        if (!isEnabled()) {
            return false
        }
        val token = request.cookies?.find { it.name == COOKIE_NAME }?.value ?: return false
        val (expiresAt, signature) = token.split(".", limit = 2).takeIf { it.size == 2 } ?: return false
        val isValidSignature = MessageDigest.isEqual(sign(expiresAt).toByteArray(), signature.toByteArray())
        val isNotExpired = expiresAt.toLongOrNull()?.let { Instant.ofEpochSecond(it).isAfter(Instant.now()) } ?: false

        return isValidSignature && isNotExpired
    }

    private fun cookie(
        value: String,
        maxAge: Duration,
        secure: Boolean,
    ): ResponseCookie =
        ResponseCookie
            .from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .path("/v1/admin")
            .maxAge(maxAge)
            .build()

    private fun sign(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingKey, "HmacSHA256"))

        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.toByteArray()))
    }

    private fun sha256(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())

    companion object {
        const val COOKIE_NAME = "ovp_admin"
        val SESSION_DURATION: Duration = Duration.ofHours(12)
        val LOCK_DURATION: Duration = Duration.ofMinutes(15)
        const val MAX_FAILED_ATTEMPTS = 10
    }
}

@Component
class AdminAuthenticationFilter(
    private val adminAuthentication: AdminAuthentication,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)

        return !path.startsWith("/v1/admin/") || path == "/v1/admin/login" || request.method == "OPTIONS"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (!adminAuthentication.isAuthenticated(request)) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"code":"UNAUTHORIZED","message":"Sign in as admin."}""")
            return
        }
        filterChain.doFilter(request, response)
    }
}
