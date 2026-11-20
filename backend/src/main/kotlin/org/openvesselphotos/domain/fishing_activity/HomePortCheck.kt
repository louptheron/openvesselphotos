package org.openvesselphotos.domain.fishing_activity

import java.time.LocalDate

/** An imported home port checked against the port the vessel visited most in a year, on Global Fishing Watch */
data class HomePortCheck(
    val cfr: String,
    val vesselName: String?,
    val importedLocode: String,
    /** Filled in when read */
    val importedPortName: String? = null,
    val result: HomePortCheckResult,
    /** The port of the map the vessel visited most, when on the map */
    val gfwLocode: String?,
    /** As named by Global Fishing Watch */
    val gfwPortName: String?,
    val visitsTotal: Int,
    val visitsAtGfwPort: Int,
    /** Visits to the imported home port in the year */
    val visitsAtImported: Int,
    /** Between the imported home port and the port visited most */
    val distanceKm: Double?,
    val checkedOn: LocalDate,
)

enum class HomePortCheckResult {
    /** The port visited most is the imported home port */
    AGREES,

    /** The port visited most is another port nearby (within [NEARBY_KM]), e.g. Locmiquélic for Lorient */
    NEARBY,

    /** The port visited most is another port, further away: the imported home port is to double check */
    DISAGREES,

    /** The port visited most is not on the map, e.g. abroad */
    NOT_ON_THE_MAP,

    /** Found, with too few port visits in the year */
    TOO_FEW_VISITS,

    /** Not found on Global Fishing Watch, e.g. without AIS */
    NOT_FOUND,
    ;

    companion object {
        const val NEARBY_KM = 15.0
    }
}
