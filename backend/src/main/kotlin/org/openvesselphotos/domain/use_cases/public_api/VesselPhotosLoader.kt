package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.springframework.stereotype.Component

/** Attaches approved photos to vessels with a single query. */
@Component
class VesselPhotosLoader(
    private val photoRepository: PhotoRepository,
) {
    fun load(vessels: List<Vessel>): List<VesselWithPhotos> {
        val photosByVesselId =
            photoRepository
                .findApprovedByVesselIds(
                    vessels.mapNotNull { it.id },
                ).groupBy { it.vesselId }

        return vessels.map { VesselWithPhotos(it, photosByVesselId[it.id].orEmpty()) }
    }
}
