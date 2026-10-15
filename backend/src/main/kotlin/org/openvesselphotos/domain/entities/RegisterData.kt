package org.openvesselphotos.domain.entities

import java.time.LocalDate

/** Technical data of a vessel, from the EU fleet register. */
data class RegisterData(
    /** FAO code (ISSCFV), e.g. TO for a trawler; FX when not specified */
    val vesselType: String? = null,
    val yearBuilt: Int? = null,
    /** Length overall */
    val lengthMeters: Double? = null,
    val enginePowerKw: Double? = null,
    /** Gross tonnage */
    val tonnageGt: Double? = null,
    /** FAO code (ISSCFG), e.g. OTB for bottom otter trawls */
    val mainGear: String? = null,
    val otherGears: List<String> = emptyList(),
    /** International radio call sign (IRCS) */
    val callSign: String? = null,
    /** Day of the last import of the register that gave this data */
    val importedOn: LocalDate? = null,
) {
    val isEmpty: Boolean get() = copy(importedOn = null) == RegisterData()
}
