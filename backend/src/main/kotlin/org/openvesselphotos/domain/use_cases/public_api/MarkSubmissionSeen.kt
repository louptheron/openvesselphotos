package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

/** Acknowledges that the contributor has seen the review of their photo. Can be called again. */
@UseCase
class MarkSubmissionSeen(
    private val photoRepository: PhotoRepository,
) {
    @Transactional
    fun execute(
        apiKeyId: Int,
        photoId: Int,
    ) {
        // Another API key's photos are not found, so that their ids cannot be probed
        val photo =
            photoRepository.findById(photoId)?.takeIf { it.apiKeyId == apiKeyId }
                ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No submission $photoId.")
        if (photo.status == PhotoStatus.PENDING) {
            throw BackendUsageException(BackendUsageErrorCode.CONFLICT, "Submission $photoId is not reviewed yet.")
        }
        if (photo.seenAt == null) {
            photoRepository.save(photo.copy(seenAt = ZonedDateTime.now()))
        }
    }
}
