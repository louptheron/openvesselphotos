package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Licence
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.images.ImageProcessor
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.VesselRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

data class PhotoSubmission(
    val key: VesselKey,
    val image: ByteArray,
    val credit: String,
    val licence: Licence,
    val takenOn: LocalDate?,
    val place: String?,
    val contributorEmail: String?,
    val vesselName: String?,
    val apiKeyId: Int? = null,
)

@UseCase
class SubmitPhoto(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
    private val imageProcessor: ImageProcessor,
) {
    @Transactional
    fun execute(submission: PhotoSubmission): Photo {
        val credit = submission.credit.trim()
        if (credit.isEmpty() || credit.length > 100) {
            throw BackendUsageException(
                BackendUsageErrorCode.INVALID_INPUT,
                "A credit (1 to 100 characters) is required.",
            )
        }
        if (submission.takenOn != null && submission.takenOn.isAfter(LocalDate.now().plusDays(1))) {
            throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "The date taken is in the future.")
        }
        val email = submission.contributorEmail?.trim()?.ifEmpty { null }
        if (email != null && (email.length > 254 || !email.contains("@"))) {
            throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "Invalid email.")
        }

        val image = imageProcessor.process(submission.image)
        val vessel = vesselRepository.findByKey(submission.key)

        val photo =
            photoRepository.save(
                Photo(
                    vesselId = vessel?.id,
                    status = PhotoStatus.PENDING,
                    submittedKey = submission.key,
                    submittedVesselName =
                        submission.vesselName
                            ?.trim()
                            ?.take(100)
                            ?.ifEmpty { null },
                    credit = credit,
                    licence = submission.licence,
                    takenOn = submission.takenOn,
                    place =
                        submission.place
                            ?.trim()
                            ?.take(200)
                            ?.ifEmpty { null },
                    contributorEmail = email,
                    width = image.width,
                    height = image.height,
                    apiKeyId = submission.apiKeyId,
                ),
            )
        image.files.forEach { (size, content) -> photoRepository.saveFile(requireNotNull(photo.id), size, content) }

        return photo
    }
}
