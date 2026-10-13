package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.ApiKey
import org.openvesselphotos.domain.entities.ApiKeyStatus
import org.openvesselphotos.domain.repositories.ApiKeyRepository

@UseCase
class AuthenticateApiKey(
    private val apiKeyRepository: ApiKeyRepository,
) {
    /** Returns the active API key matching [key], or null. */
    fun execute(key: String): ApiKey? {
        val apiKey =
            apiKeyRepository
                .findByKeyHash(ApiKey.hash(key.trim()))
                ?.takeIf { it.status == ApiKeyStatus.ACTIVE }
                ?: return null
        apiKeyRepository.markUsed(requireNotNull(apiKey.id))

        return apiKey
    }
}
