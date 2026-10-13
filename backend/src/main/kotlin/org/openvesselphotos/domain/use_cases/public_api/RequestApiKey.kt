package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.ApiKey
import org.openvesselphotos.domain.entities.ApiKeyStatus
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.ApiKeyRepository

@UseCase
class RequestApiKey(
    private val apiKeyRepository: ApiKeyRepository,
) {
    fun execute(
        name: String,
        email: String,
        organisation: String?,
        intendedUse: String,
    ): ApiKey {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedUse = intendedUse.trim()
        if (trimmedName.isEmpty() || trimmedName.length > 100) {
            throw BackendUsageException(
                BackendUsageErrorCode.INVALID_INPUT,
                "A name (1 to 100 characters) is required.",
            )
        }
        if (trimmedEmail.length > 254 || !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(trimmedEmail)) {
            throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "A valid email is required.")
        }
        if (trimmedUse.length !in 10..1000) {
            throw BackendUsageException(
                BackendUsageErrorCode.INVALID_INPUT,
                "Describe your use of the API (10 to 1000 characters).",
            )
        }

        return apiKeyRepository.save(
            ApiKey(
                name = trimmedName,
                email = trimmedEmail,
                organisation = organisation?.trim()?.take(200)?.ifEmpty { null },
                intendedUse = trimmedUse,
                status = ApiKeyStatus.PENDING,
            ),
        )
    }
}
