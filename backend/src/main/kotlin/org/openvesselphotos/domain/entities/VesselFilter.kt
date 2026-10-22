package org.openvesselphotos.domain.entities

/** Filters of the vessel directory; null for any value. */
data class VesselFilter(
    /** ISO 3166-1 alpha-3, see [Flags] */
    val flag: String? = null,
    /** Port of registration ("quartier d'immatriculation" in France) */
    val port: String? = null,
    /** EU fleet register vessel type code, e.g. "FX" */
    val vesselType: String? = null,
    /** FAO code of the main gear, e.g. "OTB" */
    val mainGear: String? = null,
) {
    val isEmpty: Boolean get() = this == VesselFilter()
}

/** A value of a filter, and the number of vessels with photos that have it */
data class FilterOption(
    val value: String,
    val count: Int,
)

/** The values the vessel directory can be filtered by, most frequent first */
data class VesselFilterOptions(
    val flags: List<FilterOption>,
    val ports: List<FilterOption>,
    val vesselTypes: List<FilterOption>,
    val mainGears: List<FilterOption>,
)
