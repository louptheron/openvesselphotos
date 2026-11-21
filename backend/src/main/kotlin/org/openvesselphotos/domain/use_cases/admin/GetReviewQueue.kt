package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.ApiKey
import org.openvesselphotos.domain.entities.ApiKeyStatus
import org.openvesselphotos.domain.entities.HomePort
import org.openvesselphotos.domain.entities.HomePortCorrection
import org.openvesselphotos.domain.entities.KeyLinkRequest
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.entities.Report
import org.openvesselphotos.domain.entities.ReportStatus
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.repositories.ApiKeyRepository
import org.openvesselphotos.domain.repositories.HomePortCorrectionRepository
import org.openvesselphotos.domain.repositories.KeyLinkRequestRepository
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.PortRepository
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

data class PendingHomePortCorrection(
    val correction: HomePortCorrection,
    /** Null when Open Vessel Photos does not know the vessel: it is only on the ports site */
    val vessel: Vessel?,
    /** The vessel's home port now, null without one */
    val currentHomePort: HomePort?,
    val currentPort: Port?,
    val suggestedPort: Port,
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
    val homePorts: List<PendingHomePortCorrection> = emptyList(),
)

@UseCase
class GetReviewQueue(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
    private val keyLinkRequestRepository: KeyLinkRequestRepository,
    private val reportRepository: ReportRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val homePortCorrectionRepository: HomePortCorrectionRepository,
    private val portRepository: PortRepository,
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
            homePorts = pendingHomePorts(),
        )
    }

    private fun pendingHomePorts(): List<PendingHomePortCorrection> {
        val ports = mutableMapOf<String, Port?>()

        fun port(locode: String) = ports.getOrPut(locode) { portRepository.findByLocode(locode) }

        return homePortCorrectionRepository.findByStatus(ReviewStatus.PENDING).mapNotNull { correction ->
            val currentHomePort = portRepository.findHomePort(correction.cfr)
            PendingHomePortCorrection(
                correction = correction,
                vessel = vesselRepository.findByKey(VesselKey.of(KeyType.CFR, correction.cfr)),
                currentHomePort = currentHomePort,
                currentPort = currentHomePort?.let { port(it.portLocode) },
                suggestedPort = port(correction.portLocode) ?: return@mapNotNull null,
            )
        }
    }
}
