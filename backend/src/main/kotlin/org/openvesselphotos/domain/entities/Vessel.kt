package org.openvesselphotos.domain.entities

import java.time.ZonedDateTime

data class Vessel(
    val id: Int? = null,
    val cfr: String?,
    val mmsi: String?,
    val name: String?,
    /** Marking painted on the hull, e.g. "LO 752221" */
    val registration: String? = null,
    /** Port of registration */
    val port: String? = null,
    val register: RegisterData = RegisterData(),
    val wikidata: WikidataData = WikidataData(),
    val createdAt: ZonedDateTime? = null,
) {
    /**
     * Named, with its type, year and length known: enough for a page of its own before any photo. Same rule as
     * [org.openvesselphotos.domain.repositories.VesselRepository.findDescribedWithoutApprovedPhotos].
     */
    val isDescribed: Boolean
        get() =
            name != null &&
                register.vesselType != null &&
                register.vesselType != UNSPECIFIED_VESSEL_TYPE &&
                register.yearBuilt != null &&
                register.lengthMeters != null

    /** ISO 3166-1 alpha-3 code, the first three letters of the CFR. */
    val flag: String? get() = cfr?.take(3)

    fun keyValue(type: KeyType): String? =
        when (type) {
            KeyType.CFR -> cfr
            KeyType.MMSI -> mmsi
        }

    fun withKey(key: VesselKey): Vessel =
        when (key.type) {
            KeyType.CFR -> copy(cfr = key.value)
            KeyType.MMSI -> copy(mmsi = key.value)
        }

    companion object {
        /** FAO vessel type code of the EU fleet register when the type is not specified */
        const val UNSPECIFIED_VESSEL_TYPE = "FX"

        fun create(
            key: VesselKey,
            name: String?,
        ): Vessel = Vessel(cfr = null, mmsi = null, name = name).withKey(key)
    }
}
