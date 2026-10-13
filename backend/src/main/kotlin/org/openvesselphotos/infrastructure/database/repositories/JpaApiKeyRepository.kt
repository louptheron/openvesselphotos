package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.ApiKey
import org.openvesselphotos.domain.entities.ApiKeyStatus
import org.openvesselphotos.domain.repositories.ApiKeyRepository
import org.openvesselphotos.infrastructure.database.entities.ApiKeyEntity
import org.openvesselphotos.infrastructure.database.repositories.interfaces.DBApiKeyRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaApiKeyRepository(
    private val dbApiKeyRepository: DBApiKeyRepository,
) : ApiKeyRepository {
    override fun findById(id: Int): ApiKey? = dbApiKeyRepository.findById(id).orElse(null)?.toApiKey()

    override fun findByIds(ids: Collection<Int>): List<ApiKey> =
        dbApiKeyRepository.findAllById(ids).map { it.toApiKey() }

    override fun findByKeyHash(keyHash: String): ApiKey? = dbApiKeyRepository.findByKeyHash(keyHash)?.toApiKey()

    override fun findByStatus(status: ApiKeyStatus): List<ApiKey> =
        dbApiKeyRepository.findByStatusOrderByCreatedAt(status).map { it.toApiKey() }

    override fun save(apiKey: ApiKey): ApiKey =
        dbApiKeyRepository.saveAndFlush(ApiKeyEntity.fromApiKey(apiKey)).toApiKey()

    @Transactional
    override fun markUsed(id: Int) {
        dbApiKeyRepository.markUsed(id)
    }
}
