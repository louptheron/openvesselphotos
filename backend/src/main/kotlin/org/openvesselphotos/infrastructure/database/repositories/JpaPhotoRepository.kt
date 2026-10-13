package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.infrastructure.database.entities.PhotoEntity
import org.openvesselphotos.infrastructure.database.entities.PhotoFileEntity
import org.openvesselphotos.infrastructure.database.entities.PhotoFileId
import org.openvesselphotos.infrastructure.database.repositories.interfaces.DBPhotoFileRepository
import org.openvesselphotos.infrastructure.database.repositories.interfaces.DBPhotoRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaPhotoRepository(
    private val dbPhotoRepository: DBPhotoRepository,
    private val dbPhotoFileRepository: DBPhotoFileRepository,
) : PhotoRepository {
    override fun findById(id: Int): Photo? = dbPhotoRepository.findById(id).orElse(null)?.toPhoto()

    override fun findByIds(ids: Collection<Int>): List<Photo> = dbPhotoRepository.findAllById(ids).map { it.toPhoto() }

    override fun findByStatus(status: PhotoStatus): List<Photo> =
        dbPhotoRepository.findByStatusOrderByCreatedAt(status).map { it.toPhoto() }

    override fun findApprovedByVesselIds(vesselIds: Collection<Int>): List<Photo> =
        if (vesselIds.isEmpty()) {
            emptyList()
        } else {
            dbPhotoRepository
                .findByVesselIdInAndStatusOrderByNumber(
                    vesselIds,
                    PhotoStatus.APPROVED,
                ).map { it.toPhoto() }
        }

    override fun findApprovedByVesselIdAndNumber(
        vesselId: Int,
        number: Int,
    ): Photo? = dbPhotoRepository.findByVesselIdAndNumberAndStatus(vesselId, number, PhotoStatus.APPROVED)?.toPhoto()

    override fun nextNumber(vesselId: Int): Int = dbPhotoRepository.nextNumber(vesselId)

    override fun save(photo: Photo): Photo {
        val saved = dbPhotoRepository.saveAndFlush(PhotoEntity.fromPhoto(photo))

        return saved.toPhoto().copy(createdAt = photo.createdAt)
    }

    override fun saveFile(
        photoId: Int,
        size: ImageSize,
        content: ByteArray,
    ) {
        dbPhotoFileRepository.save(PhotoFileEntity(PhotoFileId(photoId, size.name), content))
    }

    override fun findFile(
        photoId: Int,
        size: ImageSize,
    ): ByteArray? = dbPhotoFileRepository.findContent(photoId, size.name)

    @Transactional
    override fun deleteFiles(photoId: Int) {
        dbPhotoFileRepository.deleteByPhotoId(photoId)
    }
}
