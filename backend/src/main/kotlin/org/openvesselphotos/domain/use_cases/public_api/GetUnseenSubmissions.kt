package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.VesselRepository

data class ReviewedSubmission(
    val photo: Photo,
    /** Set for an approved photo, and for a pending or rejected one sent under a known key */
    val vessel: Vessel?,
)

/**
 * A contributor's photos waiting for review ("Your photo of LOLA was received"), and the reviews they have not seen yet
 * ("Your photo of LOLA was published"), for the application of an API key to show them. A pending photo is returned
 * until it is reviewed; a reviewed one until the application marks it as seen with [MarkSubmissionSeen].
 */
@UseCase
class GetUnseenSubmissions(
    private val photoRepository: PhotoRepository,
    private val vesselRepository: VesselRepository,
) {
    fun execute(
        apiKeyId: Int,
        contributorId: String,
    ): List<ReviewedSubmission> {
        val photos = photoRepository.findUnseen(apiKeyId, contributorId.trim())
        val vesselsById = vesselRepository.findByIds(photos.mapNotNull { it.vesselId }.distinct()).associateBy { it.id }

        return photos.map { ReviewedSubmission(it, it.vesselId?.let(vesselsById::get)) }
    }
}
