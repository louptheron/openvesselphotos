package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.ApiKey
import org.openvesselphotos.domain.entities.ApiKeyStatus
import org.openvesselphotos.domain.entities.KeyLinkRequest
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.Report
import org.openvesselphotos.domain.entities.ReportStatus
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.repositories.ApiKeyRepository
import org.openvesselphotos.domain.repositories.KeyLinkRequestRepository
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.ReportRepository
import org.openvesselphotos.domain.repositories.VesselRepository

data class PendingPhoto(
    val photo: Photo,
    /** Null when the photo would create a new vessel. */
    val vessel: Vessel?,
    /** The API key the photo was sent with, if any. */
    val apiKey: ApiKey?,
)

data class PendingKeyLink(
    val request: KeyLinkRequest,
    val vessel: Vessel,
)

data class OpenReport(
    val report: Report,
    val photo: Photo,
    val vessel: Vessel,
)

data class ReviewQueue(
    val photos: List<PendingPhoto>,
    val keyLinks: List<PendingKeyLink>,
    val reports: List<OpenReport>,
    val apiKeyRequests: List<ApiKey>,
)

@UseCase
class GetReviewQueue(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
    private val keyLinkRequestRepository: KeyLinkRequestRepository,
    private val reportRepository: ReportRepository,
    private val apiKeyRepository: ApiKeyRepository,
) {
    fun execute(): ReviewQueue {
        val photos = photoRepository.findByStatus(PhotoStatus.PENDING)
        val keyLinks = keyLinkRequestRepository.findByStatus(ReviewStatus.PENDING)
        val reports = reportRepository.findByStatus(ReportStatus.OPEN)
        val reportedPhotos = photoRepository.findByIds(reports.map { it.photoId }).associateBy { it.id }

        val vesselIds =
            photos.mapNotNull { it.vesselId } + keyLinks.map { it.vesselId } +
                reportedPhotos.values.mapNotNull { it.vesselId }
        val vessels = vesselRepository.findByIds(vesselIds.toSet()).associateBy { it.id }
        val apiKeys = apiKeyRepository.findByIds(photos.mapNotNull { it.apiKeyId }.toSet()).associateBy { it.id }

        return ReviewQueue(
            // A photo submitted for an unknown key may have had its vessel created since.
            photos =
                photos.map {
                    PendingPhoto(
                        photo = it,
                        vessel = it.vesselId?.let(vessels::get) ?: vesselRepository.findByKey(it.submittedKey),
                        apiKey = it.apiKeyId?.let(apiKeys::get),
                    )
                },
            keyLinks =
                keyLinks.mapNotNull { request ->
                    vessels[request.vesselId]?.let { PendingKeyLink(request, it) }
                },
            reports =
                reports.mapNotNull { report ->
                    val photo = reportedPhotos[report.photoId] ?: return@mapNotNull null
                    val vessel = vessels[photo.vesselId] ?: return@mapNotNull null
                    OpenReport(report, photo, vessel)
                },
            apiKeyRequests = apiKeyRepository.findByStatus(ApiKeyStatus.PENDING),
        )
    }
}
