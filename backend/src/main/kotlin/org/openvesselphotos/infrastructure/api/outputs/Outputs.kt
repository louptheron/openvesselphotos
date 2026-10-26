package org.openvesselphotos.infrastructure.api.outputs

import org.openvesselphotos.domain.entities.Flags
import org.openvesselphotos.domain.entities.HistoryEvent
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.RegisterData
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselDetails
import org.openvesselphotos.domain.entities.VesselFilterOptions
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.entities.WikidataData
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
    /** e.g. "Photographed under its former name LE CRI DES FLOTS" */
    val note: String?,
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
            note = photo.note,
        )
    }
}

data class RegisterDataOutput(
    /** FAO code (ISSCFV), e.g. TO for a trawler; FX when not specified */
    val vesselType: String?,
    val yearBuilt: Int?,
    val lengthMeters: Double?,
    val enginePowerKw: Double?,
    val tonnageGt: Double?,
    /** FAO code (ISSCFG), e.g. OTB for bottom otter trawls */
    val mainGear: String?,
    val otherGears: List<String>,
    /** International radio call sign (IRCS) */
    val callSign: String?,
    /** Day of the last import of the register that gave this data */
    val importedOn: LocalDate?,
) {
    companion object {
        fun from(register: RegisterData): RegisterDataOutput? =
            if (register.isEmpty) {
                null
            } else {
                RegisterDataOutput(
                    vesselType = register.vesselType,
                    yearBuilt = register.yearBuilt,
                    lengthMeters = register.lengthMeters,
                    enginePowerKw = register.enginePowerKw,
                    tonnageGt = register.tonnageGt,
                    mainGear = register.mainGear,
                    otherGears = register.otherGears,
                    callSign = register.callSign,
                    importedOn = register.importedOn,
                )
            }
    }
}

data class WikidataDataOutput(
    /** e.g. "Q7868346" */
    val id: String,
    val url: String,
    val imo: String?,
    val shipTypeEn: String?,
    val shipTypeFr: String?,
    /** Year of construction, or else of entry into service */
    val yearBuilt: Int?,
    val lengthMeters: Double?,
    /** Names the vessel had or has, other than its name here, e.g. a former name */
    val otherNames: List<String>,
    /** Day of the last import that gave this data */
    val importedOn: LocalDate?,
) {
    companion object {
        fun from(wikidata: WikidataData): WikidataDataOutput? =
            wikidata.id?.let { id ->
                WikidataDataOutput(
                    id = id,
                    url = requireNotNull(wikidata.url),
                    imo = wikidata.imo,
                    shipTypeEn = wikidata.shipTypeEn,
                    shipTypeFr = wikidata.shipTypeFr,
                    yearBuilt = wikidata.yearBuilt,
                    lengthMeters = wikidata.lengthMeters,
                    otherNames = wikidata.otherNames,
                    importedOn = wikidata.importedOn,
                )
            }
    }
}

data class FilterOptionDataOutput(
    val value: String,
    val count: Int,
    /** For flags: ISO 3166-1 alpha-2, to name the country in any language */
    val countryCode: String? = null,
)

data class VesselFilterOptionsDataOutput(
    /** ISO 3166-1 alpha-3, from the CFR or else the MMSI's MID */
    val flags: List<FilterOptionDataOutput>,
    /** Ports of registration ("quartiers d'immatriculation" in France) */
    val ports: List<FilterOptionDataOutput>,
    /** EU fleet register vessel type codes */
    val vesselTypes: List<FilterOptionDataOutput>,
    /** FAO gear codes */
    val mainGears: List<FilterOptionDataOutput>,
) {
    companion object {
        fun from(options: VesselFilterOptions) =
            VesselFilterOptionsDataOutput(
                flags = options.flags.map { FilterOptionDataOutput(it.value, it.count, Flags.alpha2(it.value)) },
                ports = options.ports.map { FilterOptionDataOutput(it.value, it.count) },
                vesselTypes = options.vesselTypes.map { FilterOptionDataOutput(it.value, it.count) },
                mainGears = options.mainGears.map { FilterOptionDataOutput(it.value, it.count) },
            )
    }
}

data class VesselsPageDataOutput(
    val page: Int,
    val pageCount: Int,
    /** Vessels with photos, on all pages */
    val total: Int,
    val vessels: List<VesselSummaryDataOutput>,
)

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
    /** ISO 3166-1 alpha-2 of the flag, from the CFR or else the MMSI's MID: to name the country in any language */
    val countryCode: String?,
    /** Technical data from the EU fleet register, if imported */
    val register: RegisterDataOutput?,
    /** What Wikidata knows of the vessel, if found by its MMSI */
    val wikidata: WikidataDataOutput?,
    val photos: List<PhotoDataOutput>,
    val history: List<HistoryEventDataOutput>,
    /** Other vessels with photos, from the same port or under the same flag */
    val related: List<VesselSummaryDataOutput>,
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
            countryCode = (details.vessel.flag ?: Flags.ofMmsi(details.vessel.mmsi))?.let { Flags.alpha2(it) },
            register = RegisterDataOutput.from(details.vessel.register),
            wikidata = WikidataDataOutput.from(details.vessel.wikidata),
            photos = details.photos.map { PhotoDataOutput.from(baseUrl, details.vessel, it) },
            history = details.history.map { HistoryEventDataOutput.from(it) },
            related = details.related.map { VesselSummaryDataOutput.from(baseUrl, it) },
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
