package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.ApiKey
import org.openvesselphotos.domain.entities.ApiKeyStatus
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.ApiKeyRepository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

data class ApprovedApiKey(
    val apiKey: ApiKey,
    /** The key itself: only returned here, never stored. */
    val key: String,
)

@UseCase
class GetActiveApiKeys(
    private val apiKeyRepository: ApiKeyRepository,
) {
    fun execute(): List<ApiKey> = apiKeyRepository.findByStatus(ApiKeyStatus.ACTIVE)
}

@UseCase
class ApproveApiKeyRequest(
    private val apiKeyRepository: ApiKeyRepository,
) {
    @Transactional
    fun execute(id: Int): ApprovedApiKey {
        val request = findApiKey(apiKeyRepository, id, ApiKeyStatus.PENDING)
        val key = ApiKey.generateKey()
        val apiKey =
            apiKeyRepository.save(
                request.copy(
                    status = ApiKeyStatus.ACTIVE,
                    keyHash = ApiKey.hash(key),
                    keyPrefix = ApiKey.displayPrefix(key),
                    reviewedAt = ZonedDateTime.now(),
                ),
            )

        return ApprovedApiKey(apiKey, key)
    }
}

@UseCase
class RejectApiKeyRequest(
    private val apiKeyRepository: ApiKeyRepository,
) {
    fun execute(id: Int): ApiKey {
        val request = findApiKey(apiKeyRepository, id, ApiKeyStatus.PENDING)

        return apiKeyRepository.save(request.copy(status = ApiKeyStatus.REJECTED, reviewedAt = ZonedDateTime.now()))
    }
}

@UseCase
class RevokeApiKey(
    private val apiKeyRepository: ApiKeyRepository,
) {
    fun execute(id: Int): ApiKey {
        val apiKey = findApiKey(apiKeyRepository, id, ApiKeyStatus.ACTIVE)

        return apiKeyRepository.save(apiKey.copy(status = ApiKeyStatus.REVOKED, reviewedAt = ZonedDateTime.now()))
    }
}

private fun findApiKey(
    apiKeyRepository: ApiKeyRepository,
    id: Int,
    expectedStatus: ApiKeyStatus,
): ApiKey {
    val apiKey =
        apiKeyRepository.findById(id) ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No API key $id.")
    if (apiKey.status != expectedStatus) {
        throw BackendUsageException(BackendUsageErrorCode.CONFLICT, "API key $id is ${apiKey.status.name.lowercase()}.")
    }

    return apiKey
}
