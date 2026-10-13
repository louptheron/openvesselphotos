package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@UseCase
class RejectPhoto(
    private val photoRepository: PhotoRepository,
) {
    /** Rejects the photo and deletes its image files. */
    @Transactional
    fun execute(
        photoId: Int,
        reason: String?,
    ): Photo {
        val photo = findPendingPhoto(photoRepository, photoId)
        photoRepository.deleteFiles(photoId)

        return photoRepository.save(
            photo.copy(
                status = PhotoStatus.REJECTED,
                rejectReason = reason?.trim()?.take(200)?.ifEmpty { null },
                reviewedAt = ZonedDateTime.now(),
            ),
        )
    }
}
