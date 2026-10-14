package org.openvesselphotos.infrastructure.api.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.InetAddress

/**
 * The client's IP address. Behind a proxy that sets the client IP in a header it controls (Scalingo's router sets
 * X-Real-IP), set OPENVESSELPHOTOS_CLIENT_IP_HEADER to that header. Otherwise the TCP address is used: a header sent
 * by the client itself must never be trusted, or anyone could change their IP.
 *
 * Behind Cloudflare (OPENVESSELPHOTOS_BEHIND_CLOUDFLARE), that address is one of Cloudflare's servers: the client IP is
 * then the CF-Connecting-IP header, trusted only when the request comes from Cloudflare. A request sent to the app
 * directly (e.g. to its *.scalingo.io domain) keeps its own address, whatever header it sends.
 */
@Component
class ClientIp(
    @param:Value("\${openvesselphotos.client-ip-header}")
    private val clientIpHeader: String,
    @param:Value("\${openvesselphotos.behind-cloudflare}")
    private val isBehindCloudflare: Boolean,
) {
    fun of(request: HttpServletRequest): String {
        val proxyIp = clientIpHeader.takeIf { it.isNotBlank() }?.let { request.firstIp(it) } ?: request.remoteAddr
        if (!isBehindCloudflare || !isCloudflare(proxyIp)) {
            return proxyIp
        }

        return request.firstIp(CLOUDFLARE_HEADER) ?: proxyIp
    }

    private fun HttpServletRequest.firstIp(header: String): String? =
        getHeader(header)
            ?.substringBefore(",")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    companion object {
        const val CLOUDFLARE_HEADER = "CF-Connecting-IP"

        /** https://www.cloudflare.com/ips/ (October 2026) */
        private val CLOUDFLARE_RANGES =
            listOf(
                "173.245.48.0/20",
                "103.21.244.0/22",
                "103.22.200.0/22",
                "103.31.4.0/22",
                "141.101.64.0/18",
                "108.162.192.0/18",
                "190.93.240.0/20",
                "188.114.96.0/20",
                "197.234.240.0/22",
                "198.41.128.0/17",
                "162.158.0.0/15",
                "104.16.0.0/13",
                "104.24.0.0/14",
                "172.64.0.0/13",
                "131.0.72.0/22",
                "2400:cb00::/32",
                "2606:4700::/32",
                "2803:f800::/32",
                "2405:b500::/32",
                "2405:8100::/32",
                "2a06:98c0::/29",
                "2c0f:f248::/32",
            ).map { IpRange.of(it) }

        fun isCloudflare(ip: String): Boolean {
            val address = parseIp(ip) ?: return false

            return CLOUDFLARE_RANGES.any { it.contains(address) }
        }

        /** Parses an IP literal only: InetAddress would resolve a host name through DNS. */
        private fun parseIp(ip: String): ByteArray? =
            ip
                .takeIf { it.isNotEmpty() && it.all { char -> char.isLetterOrDigit() || char == '.' || char == ':' } }
                ?.takeIf { it.contains(':') || it.all { char -> char.isDigit() || char == '.' } }
                ?.let { runCatching { InetAddress.getByName(it).address }.getOrNull() }
    }

    private class IpRange(
        private val network: ByteArray,
        private val prefixLength: Int,
    ) {
        fun contains(address: ByteArray): Boolean {
            if (address.size != network.size) {
                return false
            }

            return (0 until prefixLength).all { bit ->
                val mask = 0x80 ushr (bit % 8)
                (address[bit / 8].toInt() and mask) == (network[bit / 8].toInt() and mask)
            }
        }

        companion object {
            fun of(cidr: String): IpRange {
                val (ip, prefixLength) = cidr.split("/")

                return IpRange(InetAddress.getByName(ip).address, prefixLength.toInt())
            }
        }
    }
}
