package org.openvesselphotos.infrastructure.api.outputs

import org.openvesselphotos.domain.entities.Coverage
import org.openvesselphotos.domain.entities.HistoryEvent
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselDetails
import org.openvesselphotos.domain.entities.VesselWithPhotos
import java.time.LocalDate
import java.time.ZonedDateTime

/** Image URLs of a photo, one per key the vessel has. */
data class PhotoUrlsDataOutput(
    val cfr: String?,
    val mmsi: String?,
) {
    companion object {
        fun from(
            baseUrl: String,
            vessel: Vessel,
            photo: Photo,
        ): PhotoUrlsDataOutput {
            fun url(type: KeyType) =
                vessel.keyValue(type)?.let { "$baseUrl/v1/${type.pathName}/$it/photos/${photo.number}.jpg" }

            return PhotoUrlsDataOutput(cfr = url(KeyType.CFR), mmsi = url(KeyType.MMSI))
        }
    }
}

data class PhotoDataOutput(
    val n: Int,
    val urls: PhotoUrlsDataOutput,
    val credit: String,
    val licence: String,
    val licenceName: String,
    val attribution: String,
    val takenOn: LocalDate?,
    val place: String?,
    val width: Int,
    val height: Int,
    /** Page of the original photo, for a photo imported from Wikimedia Commons or Flickr */
    val source: String?,
) {
    companion object {
        fun from(
            baseUrl: String,
            vessel: Vessel,
            photo: Photo,
        ) = PhotoDataOutput(
            n = requireNotNull(photo.number),
            urls = PhotoUrlsDataOutput.from(baseUrl, vessel, photo),
            credit = photo.credit,
            licence = photo.licence.spdx,
            licenceName = photo.licence.label,
            attribution =
                if (photo.licence.requiresCredit) {
                    "Photo: ${photo.credit} / Open Vessel Photos, ${photo.licence.label}"
                } else {
                    "Photo: ${photo.credit} / Open Vessel Photos (${photo.licence.label})"
                },
            takenOn = photo.takenOn,
            place = photo.place,
            width = photo.width,
            height = photo.height,
            source = photo.sourceUrl,
        )
    }
}

data class CoverageDataOutput(
    val registerVessels: Int,
    val registerPhotographed: Int,
    val photographedVessels: Int,
    val photos: Int,
) {
    companion object {
        fun from(coverage: Coverage) =
            CoverageDataOutput(
                registerVessels = coverage.registerVessels,
                registerPhotographed = coverage.registerPhotographed,
                photographedVessels = coverage.photographedVessels,
                photos = coverage.photos,
            )
    }
}

data class VesselSummaryDataOutput(
    val cfr: String?,
    val mmsi: String?,
    val name: String?,
    val registration: String?,
    val port: String?,
    val flag: String?,
    val photoCount: Int,
    val mainPhoto: PhotoDataOutput?,
) {
    companion object {
        fun from(
            baseUrl: String,
            vesselWithPhotos: VesselWithPhotos,
        ): VesselSummaryDataOutput {
            val vessel = vesselWithPhotos.vessel

            return VesselSummaryDataOutput(
                cfr = vessel.cfr,
                mmsi = vessel.mmsi,
                name = vessel.name,
                registration = vessel.registration,
                port = vessel.port,
                flag = vessel.flag,
                photoCount = vesselWithPhotos.photos.size,
                mainPhoto = vesselWithPhotos.photos.firstOrNull()?.let { PhotoDataOutput.from(baseUrl, vessel, it) },
            )
        }
    }
}

data class HistoryEventDataOutput(
    val date: ZonedDateTime,
    val type: String,
    /** In English; clients can build a translated text from the other fields */
    val description: String,
    val by: String?,
    val photoNumber: Int?,
    val licence: String?,
    val key: String?,
    val linkedTo: String?,
) {
    companion object {
        fun from(event: HistoryEvent) =
            HistoryEventDataOutput(
                date = event.date,
                type = event.type.name,
                description = event.description,
                by = event.by,
                photoNumber = event.photoNumber,
                licence = event.licence,
                key = event.key,
                linkedTo = event.linkedTo,
            )
    }
}

data class VesselDataOutput(
    val cfr: String?,
    val mmsi: String?,
    val name: String?,
    val registration: String?,
    val port: String?,
    val flag: String?,
    val photos: List<PhotoDataOutput>,
    val history: List<HistoryEventDataOutput>,
) {
    companion object {
        fun from(
            baseUrl: String,
            details: VesselDetails,
        ) = VesselDataOutput(
            cfr = details.vessel.cfr,
            mmsi = details.vessel.mmsi,
            name = details.vessel.name,
            registration = details.vessel.registration,
            port = details.vessel.port,
            flag = details.vessel.flag,
            photos = details.photos.map { PhotoDataOutput.from(baseUrl, details.vessel, it) },
            history = details.history.map { HistoryEventDataOutput.from(it) },
        )
    }
}

data class ExportedVesselDataOutput(
    val cfr: String?,
    val mmsi: String?,
    val name: String?,
    val photos: List<PhotoDataOutput>,
)

data class SubmissionDataOutput(
    val id: Int,
    val status: String,
)
