package org.openvesselphotos.infrastructure.api.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * The client's IP address. Behind a proxy that sets the client IP in a header it controls (Scalingo's router sets
 * X-Real-IP), set OPENVESSELPHOTOS_CLIENT_IP_HEADER to that header. Otherwise the TCP address is used: a header sent
 * by the client itself must never be trusted, or anyone could change their IP.
 */
@Component
class ClientIp(
    @param:Value("\${openvesselphotos.client-ip-header}")
    private val clientIpHeader: String,
) {
    fun of(request: HttpServletRequest): String =
        clientIpHeader
            .takeIf { it.isNotBlank() }
            ?.let { request.getHeader(it) }
            ?.substringBefore(",")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: request.remoteAddr
}
