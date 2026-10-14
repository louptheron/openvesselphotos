package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.PhotoStatus

interface PhotoRepository {
    fun findById(id: Int): Photo?

    fun findByIds(ids: Collection<Int>): List<Photo>

    fun findByStatus(status: PhotoStatus): List<Photo>

    /** Approved photos of these vessels, ordered by number. */
    fun findApprovedByVesselIds(vesselIds: Collection<Int>): List<Photo>

    fun findApprovedByVesselIdAndNumber(
        vesselId: Int,
        number: Int,
    ): Photo?

    fun nextNumber(vesselId: Int): Int

    fun existsBySourceUrl(sourceUrl: String): Boolean

    fun save(photo: Photo): Photo

    fun saveFile(
        photoId: Int,
        size: ImageSize,
        content: ByteArray,
    )

    fun findFile(
        photoId: Int,
        size: ImageSize,
    ): ByteArray?

    fun deleteFiles(photoId: Int)
}
