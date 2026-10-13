package org.openvesselphotos.domain.entities

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.ZonedDateTime
import java.util.Base64

enum class ApiKeyStatus {
    /** Requested, waiting for review */
    PENDING,
    ACTIVE,
    REJECTED,
    REVOKED,
}

/** A request for an API key, and once approved, the key (as a hash). */
data class ApiKey(
    val id: Int? = null,
    val name: String,
    val email: String,
    val organisation: String?,
    val intendedUse: String,
    val status: ApiKeyStatus,
    val keyHash: String? = null,
    val keyPrefix: String? = null,
    val createdAt: ZonedDateTime? = null,
    val reviewedAt: ZonedDateTime? = null,
    val lastUsedAt: ZonedDateTime? = null,
) {
    companion object {
        private const val PREFIX = "ovp_"
        private val random = SecureRandom()

        /** A new key: "ovp_" + 32 random URL-safe characters (192 bits). */
        fun generateKey(): String {
            val bytes = ByteArray(24).also(random::nextBytes)

            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        fun hash(key: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(key.toByteArray())
                .joinToString("") { "%02x".format(it) }

        fun displayPrefix(key: String): String = key.take(PREFIX.length + 4)
    }
}
