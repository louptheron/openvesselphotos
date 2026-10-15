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
    /** UN/LOCODE of the port of registration, e.g. FRBES */
    val portCode: String? = null,
    val register: RegisterData = RegisterData(),
    val createdAt: ZonedDateTime? = null,
) {
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
        fun create(
            key: VesselKey,
            name: String?,
        ): Vessel = Vessel(cfr = null, mmsi = null, name = name).withKey(key)
    }
}
