package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.repositories.VesselRepository

/**
 * The vessels without photo whose pages search engines may index: the first [limit] vessels described by the EU fleet
 * register (see [Vessel.isDescribed]). The limit grows every day, so that their pages are released a few at a time and
 * their indexing can be followed (see VesselsWithoutPhotosRelease).
 */
@UseCase
class GetVesselsWithoutPhotosToIndex(
    private val vesselRepository: VesselRepository,
) {
    fun execute(limit: Int): List<Vessel> =
        if (limit <= 0) emptyList() else vesselRepository.findDescribedWithoutApprovedPhotos(limit)

    fun isIndexed(
        vessel: Vessel,
        limit: Int,
    ): Boolean =
        vessel.isDescribed &&
            vessel.id != null &&
            vesselRepository.countDescribedWithoutApprovedPhotosBefore(vessel.id) < limit
}
