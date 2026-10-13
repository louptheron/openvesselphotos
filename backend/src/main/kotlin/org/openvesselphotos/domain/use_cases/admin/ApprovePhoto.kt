package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.VesselRepository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@UseCase
class ApprovePhoto(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
) {
    /** Publishes the photo, creating its vessel when the key is new. [vesselName] overrides the submitted one. */
    @Transactional
    fun execute(
        photoId: Int,
        vesselName: String?,
    ): Photo {
        val photo = findPendingPhoto(photoRepository, photoId)
        val name = vesselName?.trim()?.take(100)?.ifEmpty { null } ?: photo.submittedVesselName

        val existingVessel =
            photo.vesselId?.let { vesselRepository.findById(it) } ?: vesselRepository.findByKey(photo.submittedKey)
        val vessel =
            when {
                existingVessel == null -> vesselRepository.save(Vessel.create(photo.submittedKey, name))
                existingVessel.name == null && name != null -> vesselRepository.save(existingVessel.copy(name = name))
                else -> existingVessel
            }
        val vesselId = requireNotNull(vessel.id)

        return photoRepository.save(
            photo.copy(
                vesselId = vesselId,
                number = photoRepository.nextNumber(vesselId),
                status = PhotoStatus.APPROVED,
                reviewedAt = ZonedDateTime.now(),
            ),
        )
    }
}

internal fun findPendingPhoto(
    photoRepository: PhotoRepository,
    photoId: Int,
): Photo {
    val photo =
        photoRepository.findById(photoId)
            ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No photo $photoId.")
    if (photo.status != PhotoStatus.PENDING) {
        throw BackendUsageException(BackendUsageErrorCode.CONFLICT, "Photo $photoId was already reviewed.")
    }

    return photo
}
