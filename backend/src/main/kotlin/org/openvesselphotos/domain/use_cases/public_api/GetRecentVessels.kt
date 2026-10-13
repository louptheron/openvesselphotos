package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.repositories.VesselRepository

@UseCase
class GetRecentVessels(
    private val vesselRepository: VesselRepository,
    private val vesselPhotosLoader: VesselPhotosLoader,
) {
    fun execute(limit: Int = 12): List<VesselWithPhotos> =
        vesselPhotosLoader.load(vesselRepository.findRecentlyPhotographed(limit.coerceIn(1, 48)))
}
