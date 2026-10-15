package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Licence
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.images.ImageProcessor
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.VesselRepository
import org.openvesselphotos.domain.use_cases.findOrCreateVessel
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZonedDateTime

/** A photo to import, already validated (see [ImportPhotos]). */
data class PhotoImport(
    val line: Int,
    /** A CFR, an MMSI or both */
    val keys: List<VesselKey>,
    val vesselName: String?,
    val credit: String,
    val licence: Licence,
    val takenOn: LocalDate?,
    val sourceUrl: String,
    val imageUrl: String,
    val note: String? = null,
)

/** Publishes an imported photo without review, creating its vessel when the key is new. */
@UseCase
class PublishImportedPhoto(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
    private val imageProcessor: ImageProcessor,
) {
    @Transactional
    fun execute(
        photo: PhotoImport,
        imageBytes: ByteArray,
    ): Photo {
        val image = imageProcessor.process(imageBytes)

        val vessel = findOrCreateVessel(vesselRepository, photo.keys, photo.vesselName)
        val vesselId = requireNotNull(vessel.id)

        val saved =
            photoRepository.save(
                Photo(
                    vesselId = vesselId,
                    number = photoRepository.nextNumber(vesselId),
                    status = PhotoStatus.APPROVED,
                    submittedKey = photo.keys.first(),
                    submittedOtherKey = photo.keys.getOrNull(1),
                    submittedVesselName = photo.vesselName,
                    credit = photo.credit,
                    licence = photo.licence,
                    takenOn = photo.takenOn,
                    place = null,
                    contributorEmail = null,
                    width = image.width,
                    height = image.height,
                    reviewedAt = ZonedDateTime.now(),
                    sourceUrl = photo.sourceUrl,
                    note = photo.note,
                ),
            )
        image.files.forEach { (size, content) -> photoRepository.saveFile(requireNotNull(saved.id), size, content) }

        return saved
    }
}
