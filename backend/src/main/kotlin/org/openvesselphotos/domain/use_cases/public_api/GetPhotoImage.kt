package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.VesselRepository

data class PhotoImage(
    val photo: Photo,
    val content: ByteArray,
)

@UseCase
class GetPhotoImage(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
) {
    /** [number] null means the main photo (the lowest approved number). */
    fun execute(
        key: VesselKey,
        number: Int?,
        size: ImageSize,
    ): PhotoImage {
        val notFound =
            BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No photo for ${key.type.name} ${key.value}.")
        val vesselId = vesselRepository.findByKey(key)?.id ?: throw notFound

        val photo =
            if (number == null) {
                photoRepository.findApprovedByVesselIds(listOf(vesselId)).firstOrNull()
            } else {
                photoRepository.findApprovedByVesselIdAndNumber(vesselId, number)
            } ?: throw notFound
        val content = photoRepository.findFile(requireNotNull(photo.id), size) ?: throw notFound

        return PhotoImage(photo, content)
    }
}
