package org.openvesselphotos.domain.fishing_activity

import java.time.LocalDate

/** A vessel's call at a port, observed from its positions (AIS) */
data class PortVisit(
    /** e.g. fra-lorient */
    val portId: String,
    /** e.g. LORIENT */
    val portName: String,
    /** Country of the port, ISO 3166-1 alpha-3, e.g. FRA */
    val portFlag: String?,
    val latitude: Double,
    val longitude: Double,
)

/** A stretch of fishing of a vessel, observed from its positions (AIS) */
data class FishingEvent(
    val latitude: Double,
    val longitude: Double,
    val hours: Double,
)

/** Hours of fishing in a cell of the map, e.g. of 0.05° */
data class FishingCell(
    val latitude: Double,
    val longitude: Double,
    val hours: Double,
)

/** What a vessel is looked for by: its MMSI first, then its call sign, then its name */
data class VesselIdentity(
    val mmsi: String?,
    val callSign: String?,
    val name: String?,
    /** ISO 3166-1 alpha-3, e.g. FRA */
    val flag: String,
)

/** Fishing activity observed from the positions of vessels (Global Fishing Watch) */
interface FishingActivityClient {
    /** False without credentials: nothing is asked */
    val isEnabled: Boolean

    /**
     * A 256 px PNG tile of the fishing effort of the vessels of [flag] over the last 12 complete months, for these gear
     * types only when given (e.g. trawlers); null when the tile cannot be had.
     */
    fun fishingEffortTile(
        z: Int,
        x: Int,
        y: Int,
        flag: String,
        gearTypes: List<String>,
    ): ByteArray?

    /** The identifiers of the vessel, found by [VesselIdentity]; empty when not found, or when the name is ambiguous */
    fun findVesselIds(identity: VesselIdentity): List<String>

    fun findPortVisits(
        vesselIds: List<String>,
        from: LocalDate,
        to: LocalDate,
    ): List<PortVisit>

    fun findFishingEvents(
        vesselIds: List<String>,
        from: LocalDate,
        to: LocalDate,
    ): List<FishingEvent>
}
