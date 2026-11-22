package org.openvesselphotos.infrastructure.api.admin

import org.openvesselphotos.domain.entities.ApiKey
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.use_cases.admin.ReviewQueue
import java.time.LocalDate
import java.time.ZonedDateTime

data class AdminVesselDataOutput(
    val cfr: String?,
    val mmsi: String?,
    val name: String?,
) {
    companion object {
        fun from(vessel: Vessel) = AdminVesselDataOutput(vessel.cfr, vessel.mmsi, vessel.name)
    }
}

data class AdminPhotoDataOutput(
    val id: Int,
    val number: Int?,
    val submittedKeyType: String,
    val submittedKey: String,
    /** The key of the other type, when the contributor gave both */
    val submittedOtherKey: String?,
    val submittedVesselName: String?,
    val credit: String,
    val licence: String,
    val licenceName: String,
    val takenOn: LocalDate?,
    val place: String?,
    val contributorEmail: String?,
    val contributorId: String?,
    val width: Int,
    val height: Int,
    val createdAt: ZonedDateTime?,
) {
    companion object {
        fun from(photo: Photo) =
            AdminPhotoDataOutput(
                id = requireNotNull(photo.id),
                number = photo.number,
                submittedKeyType = photo.submittedKey.type.pathName,
                submittedKey = photo.submittedKey.value,
                submittedOtherKey = photo.submittedOtherKey?.value,
                submittedVesselName = photo.submittedVesselName,
                credit = photo.displayedCredit,
                licence = photo.licence.spdx,
                licenceName = photo.licence.label,
                takenOn = photo.takenOn,
                place = photo.place,
                contributorEmail = photo.contributorEmail,
                contributorId = photo.contributorId,
                width = photo.width,
                height = photo.height,
                createdAt = photo.createdAt,
            )
    }
}

data class ApiKeyDataOutput(
    val id: Int,
    val name: String,
    val email: String,
    val organisation: String?,
    val intendedUse: String,
    val status: String,
    val keyPrefix: String?,
    val publicId: String?,
    val createdAt: ZonedDateTime?,
    val reviewedAt: ZonedDateTime?,
    val lastUsedAt: ZonedDateTime?,
) {
    companion object {
        fun from(apiKey: ApiKey) =
            ApiKeyDataOutput(
                id = requireNotNull(apiKey.id),
                name = apiKey.name,
                email = apiKey.email,
                organisation = apiKey.organisation,
                intendedUse = apiKey.intendedUse,
                status = apiKey.status.name,
                keyPrefix = apiKey.keyPrefix,
                publicId = apiKey.publicId,
                createdAt = apiKey.createdAt,
                reviewedAt = apiKey.reviewedAt,
                lastUsedAt = apiKey.lastUsedAt,
            )
    }
}

data class ApprovedApiKeyDataOutput(
    val key: String,
    val apiKey: ApiKeyDataOutput,
)

data class PendingPhotoDataOutput(
    val photo: AdminPhotoDataOutput,
    val vessel: AdminVesselDataOutput?,
    /** Name of the API key the photo was sent with, if any. */
    val apiKeyName: String?,
)

data class PendingKeyLinkDataOutput(
    val id: Int,
    val keyType: String,
    val key: String,
    val contributor: String?,
    val createdAt: ZonedDateTime?,
    val vessel: AdminVesselDataOutput,
)

data class AdminPortDataOutput(
    val locode: String,
    val name: String,
)

data class PendingHomePortDataOutput(
    val id: Int,
    val cfr: String,
    /** From Open Vessel Photos, else as named on the ports site */
    val vesselName: String?,
    /** Whether Open Vessel Photos has a page for the vessel */
    val isKnownVessel: Boolean,
    val currentPort: AdminPortDataOutput?,
    /** IMPORT, GFW, REGISTER, MANUAL or RETURNS */
    val currentSource: String?,
    val suggestedPort: AdminPortDataOutput,
    val comment: String?,
    val contributor: String?,
    val createdAt: ZonedDateTime?,
)

data class OpenReportDataOutput(
    val id: Int,
    val reason: String,
    val message: String?,
    val createdAt: ZonedDateTime?,
    val photo: AdminPhotoDataOutput,
    val vessel: AdminVesselDataOutput,
)

data class ReviewQueueDataOutput(
    val photos: List<PendingPhotoDataOutput>,
    val keyLinks: List<PendingKeyLinkDataOutput>,
    val reports: List<OpenReportDataOutput>,
    val apiKeyRequests: List<ApiKeyDataOutput>,
    val homePorts: List<PendingHomePortDataOutput>,
) {
    companion object {
        fun from(queue: ReviewQueue) =
            ReviewQueueDataOutput(
                photos =
                    queue.photos.map {
                        PendingPhotoDataOutput(
                            photo = AdminPhotoDataOutput.from(it.photo),
                            vessel = it.vessel?.let(AdminVesselDataOutput::from),
                            apiKeyName = it.apiKey?.name,
                        )
                    },
                keyLinks =
                    queue.keyLinks.map {
                        PendingKeyLinkDataOutput(
                            id = requireNotNull(it.request.id),
                            keyType = it.request.key.type.pathName,
                            key = it.request.key.value,
                            contributor = it.request.contributor,
                            createdAt = it.request.createdAt,
                            vessel = AdminVesselDataOutput.from(it.vessel),
                        )
                    },
                reports =
                    queue.reports.map {
                        OpenReportDataOutput(
                            id = requireNotNull(it.report.id),
                            reason = it.report.reason.name,
                            message = it.report.message,
                            createdAt = it.report.createdAt,
                            photo = AdminPhotoDataOutput.from(it.photo),
                            vessel = AdminVesselDataOutput.from(it.vessel),
                        )
                    },
                apiKeyRequests = queue.apiKeyRequests.map(ApiKeyDataOutput::from),
                homePorts =
                    queue.homePorts.map {
                        PendingHomePortDataOutput(
                            id = requireNotNull(it.correction.id),
                            cfr = it.correction.cfr,
                            vesselName = it.vessel?.name ?: it.currentHomePort?.vesselName,
                            isKnownVessel = it.vessel != null,
                            currentPort = it.currentPort?.let { port -> AdminPortDataOutput(port.locode, port.name) },
                            currentSource = it.currentHomePort?.source?.name,
                            suggestedPort = AdminPortDataOutput(it.suggestedPort.locode, it.suggestedPort.name),
                            comment = it.correction.comment,
                            contributor = it.correction.contributor,
                            createdAt = it.correction.createdAt,
                        )
                    },
            )
    }
}
