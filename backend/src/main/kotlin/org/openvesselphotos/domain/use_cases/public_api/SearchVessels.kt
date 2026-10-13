package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.repositories.VesselRepository

@UseCase
class SearchVessels(
    private val vesselRepository: VesselRepository,
    private val vesselPhotosLoader: VesselPhotosLoader,
) {
    fun execute(query: String): List<VesselWithPhotos> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) {
            return emptyList()
        }

        val exactMatch = VesselKey.detect(trimmedQuery)?.let { vesselRepository.findByKey(it) }
        val matches = vesselRepository.search(trimmedQuery, MAX_RESULTS)
        val vessels = (listOfNotNull(exactMatch) + matches).distinctBy { it.id }.take(MAX_RESULTS)

        return vesselPhotosLoader.load(vessels)
    }

    companion object {
        const val MAX_RESULTS = 24
    }
}
