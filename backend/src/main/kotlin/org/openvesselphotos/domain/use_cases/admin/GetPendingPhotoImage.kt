package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.PhotoRepository

/** Any photo still holding files (pending, approved or reported), by its internal id. */
@UseCase
class GetPendingPhotoImage(
    private val photoRepository: PhotoRepository,
) {
    fun execute(
        photoId: Int,
        size: ImageSize,
    ): ByteArray =
        photoRepository.findFile(photoId, size)
            ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No image for photo $photoId.")
}
