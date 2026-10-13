package org.openvesselphotos.domain.exceptions

enum class BackendUsageErrorCode {
    INVALID_KEY,
    INVALID_INPUT,
    INVALID_IMAGE,
    NOT_FOUND,
    CONFLICT,
    UNAUTHORIZED,
    TOO_MANY_REQUESTS,
}

class BackendUsageException(
    val code: BackendUsageErrorCode,
    override val message: String,
) : RuntimeException(message)
