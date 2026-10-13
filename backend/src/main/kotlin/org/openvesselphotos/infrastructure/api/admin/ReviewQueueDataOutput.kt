package org.openvesselphotos.infrastructure.api.admin

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
    val submittedVesselName: String?,
    val credit: String,
    val licence: String,
    val licenceName: String,
    val takenOn: LocalDate?,
    val place: String?,
    val contributorEmail: String?,
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
                submittedVesselName = photo.submittedVesselName,
                credit = photo.credit,
                licence = photo.licence.spdx,
                licenceName = photo.licence.label,
                takenOn = photo.takenOn,
                place = photo.place,
                contributorEmail = photo.contributorEmail,
                width = photo.width,
                height = photo.height,
                createdAt = photo.createdAt,
            )
    }
}

data class PendingPhotoDataOutput(
    val photo: AdminPhotoDataOutput,
    val vessel: AdminVesselDataOutput?,
)

data class PendingKeyLinkDataOutput(
    val id: Int,
    val keyType: String,
    val key: String,
    val contributor: String?,
    val createdAt: ZonedDateTime?,
    val vessel: AdminVesselDataOutput,
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
) {
    companion object {
        fun from(queue: ReviewQueue) =
            ReviewQueueDataOutput(
                photos =
                    queue.photos.map {
                        PendingPhotoDataOutput(
                            AdminPhotoDataOutput.from(it.photo),
                            it.vessel?.let(AdminVesselDataOutput::from),
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
            )
    }
}
