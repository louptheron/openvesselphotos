package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.repositories.VesselRepository

/** A page of the vessels of a port: those photographed, or those still waiting for a photo. */
@UseCase
class GetPortVessels(
    private val vesselRepository: VesselRepository,
    private val vesselPhotosLoader: VesselPhotosLoader,
) {
    fun execute(
        code: String,
        isPhotographed: Boolean,
        page: Int,
    ): List<VesselWithPhotos> {
        val vessels =
            vesselRepository.findByPortCode(
                code.uppercase(),
                isPhotographed,
                page.coerceAtLeast(0) * PAGE_SIZE,
                PAGE_SIZE,
            )

        return if (isPhotographed) {
            vesselPhotosLoader.load(
                vessels,
            )
        } else {
            vessels.map { VesselWithPhotos(it, emptyList()) }
        }
    }

    companion object {
        const val PAGE_SIZE = 48
    }
}
