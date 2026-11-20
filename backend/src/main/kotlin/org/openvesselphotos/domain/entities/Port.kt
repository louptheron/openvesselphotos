package org.openvesselphotos.domain.entities

/** A fishing port, by its UN/LOCODE (e.g. FRQUY for Erquy). */
data class Port(
    val locode: String,
    val name: String,
    /** ISO 3166-1 alpha-2, e.g. FR */
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class PortWithVesselCount(
    val port: Port,
    /** Vessels based at the port */
    val vesselCount: Int,
    /** Vessels based at the port by main gear (FAO code, e.g. OTB), for those whose gear is known */
    val vesselCountByGear: Map<String, Int> = emptyMap(),
    /** The fish auction of the port, when it has one */
    val auction: AuctionTotal? = null,
)

/** The port a vessel is based at, observed from its positions. */
data class HomePort(
    val cfr: String,
    /** As named in the observations, for vessels not known by Open Vessel Photos */
    val vesselName: String?,
    val portLocode: String,
    val observationsTotal: Int,
    /** Observations of the vessel at this port, among [observationsTotal] */
    val observationsAtPort: Int,
    /** Mean distance to the port of the observations at it */
    val meanDistanceKm: Double?,
)

/** The company owning a vessel (armement), from a public source. */
data class VesselOwner(
    val cfr: String,
    /** e.g. France Pélagique */
    val name: String,
    /** e.g. Cornelis Vrolijk */
    val group: String?,
    val sourceUrl: String,
)

/** A vessel based at a port, with its photos when Open Vessel Photos knows it, and its owner when known. */
data class PortVessel(
    val homePort: HomePort,
    val vessel: VesselWithPhotos?,
    val owner: VesselOwner? = null,
)

data class PortDetails(
    val port: Port,
    val vessels: List<PortVessel>,
    /** The fish auction of the port, when it has one */
    val auction: PortAuction? = null,
)

/** A vessel found by its name or CFR, with the port it is based at */
data class HomePortSearchResult(
    val cfr: String,
    val name: String?,
    val portLocode: String,
    val portName: String,
)
