package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.ApiKey
import org.openvesselphotos.domain.entities.ApiKeyStatus

interface ApiKeyRepository {
    fun findById(id: Int): ApiKey?

    fun findByIds(ids: Collection<Int>): List<ApiKey>

    fun findByKeyHash(keyHash: String): ApiKey?

    fun findByStatus(status: ApiKeyStatus): List<ApiKey>

    fun save(apiKey: ApiKey): ApiKey

    fun markUsed(id: Int)
}
