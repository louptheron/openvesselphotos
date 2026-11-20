package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.HomePortSearchResult
import org.openvesselphotos.domain.repositories.PortRepository

/** The vessels of the ports site by name or CFR, with their port: at least 2 characters, 10 results at most */
@UseCase
class SearchPortVessels(
    private val portRepository: PortRepository,
) {
    fun execute(query: String): List<HomePortSearchResult> {
        val trimmed = query.trim().take(MAX_QUERY_LENGTH)
        if (trimmed.length < MIN_QUERY_LENGTH) {
            return emptyList()
        }

        return portRepository.searchVessels(trimmed, MAX_RESULTS)
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 2
        private const val MAX_QUERY_LENGTH = 50
        private const val MAX_RESULTS = 10
    }
}
