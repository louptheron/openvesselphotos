package org.openvesselphotos.domain.entities

/** How much of the library is photographed. */
data class Coverage(
    /** Vessels of the EU fleet register (with a CFR), imported or photographed */
    val registerVessels: Int,
    /** Of those, the vessels with at least one published photo */
    val registerPhotographed: Int,
    /** All the vessels with at least one published photo, with a CFR or an MMSI only */
    val photographedVessels: Int,
    val photos: Int,
)
