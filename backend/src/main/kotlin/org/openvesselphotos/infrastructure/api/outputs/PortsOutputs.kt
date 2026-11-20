package org.openvesselphotos.infrastructure.api.outputs

import org.openvesselphotos.domain.entities.AuctionTotal
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.entities.PortAuction
import org.openvesselphotos.domain.entities.PortDetails
import org.openvesselphotos.domain.entities.PortVessel
import org.openvesselphotos.domain.entities.PortWithVesselCount
import org.openvesselphotos.infrastructure.web.PagesController

data class PortDataOutput(
    val locode: String,
    val name: String,
    val country: String,
    /** WGS 84; null until the port's position is imported */
    val latitude: Double?,
    val longitude: Double?,
    val vesselCount: Int,
    /** Vessels by main gear (FAO code, e.g. OTB for bottom otter trawls), for those whose gear is known */
    val gearCounts: Map<String, Int>,
    /** The fish auction of the port, when it has one: its sales of all species in its last year */
    val auction: AuctionTotalDataOutput?,
    /** In the URL of its page on the ports site, e.g. "le-guilvinec" for /ports/le-guilvinec; null in a port's details */
    val slug: String? = null,
) {
    companion object {
        fun from(portWithVesselCount: PortWithVesselCount) =
            from(
                portWithVesselCount.port,
                portWithVesselCount.vesselCount,
                portWithVesselCount.vesselCountByGear,
                portWithVesselCount.auction,
            ).copy(slug = portWithVesselCount.slug)

        fun from(
            port: Port,
            vesselCount: Int,
            gearCounts: Map<String, Int> = emptyMap(),
            auction: AuctionTotal? = null,
        ) = PortDataOutput(
            locode = port.locode,
            name = port.name,
            country = port.country,
            latitude = port.latitude,
            longitude = port.longitude,
            vesselCount = vesselCount,
            gearCounts = gearCounts,
            auction =
                auction?.let {
                    AuctionTotalDataOutput(it.auction, it.year, it.quantityTonnes, it.valueThousandEuros)
                },
        )
    }
}

data class AuctionTotalDataOutput(
    val name: String,
    val year: Int,
    val quantityTonnes: Double,
    val valueThousandEuros: Double,
)

data class PortVesselDataOutput(
    val cfr: String,
    val name: String?,
    val registration: String?,
    /** FAO code (ISSCFV), from the EU fleet register */
    val vesselType: String?,
    val lengthMeters: Double?,
    val yearBuilt: Int?,
    /** Gross tonnage */
    val tonnageGt: Double?,
    /** FAO code (ISSCFG) */
    val mainGear: String?,
    val observationsTotal: Int,
    val observationsAtPort: Int,
    val meanDistanceKm: Double?,
    /** IMPORT: from the imported observations; GFW: the port it visited most in a year, on Global Fishing Watch */
    val homePortSource: String,
    /** The vessel's page on Open Vessel Photos, when it has one */
    val pageUrl: String?,
    val photoCount: Int,
    val mainPhoto: PhotoDataOutput?,
    /** The company owning the vessel, from a public source */
    val owner: VesselOwnerDataOutput?,
) {
    companion object {
        /** [baseUrl]: the base URL of Open Vessel Photos, which serves the vessel pages and photos */
        fun from(
            baseUrl: String,
            portVessel: PortVessel,
        ): PortVesselDataOutput {
            val homePort = portVessel.homePort
            val vessel = portVessel.vessel?.vessel
            val photos = portVessel.vessel?.photos.orEmpty()

            return PortVesselDataOutput(
                cfr = homePort.cfr,
                name = vessel?.name ?: homePort.vesselName,
                registration = vessel?.registration,
                vesselType = vessel?.register?.vesselType,
                lengthMeters = vessel?.register?.lengthMeters,
                yearBuilt = vessel?.register?.yearBuilt,
                tonnageGt = vessel?.register?.tonnageGt,
                mainGear = vessel?.register?.mainGear,
                observationsTotal = homePort.observationsTotal,
                observationsAtPort = homePort.observationsAtPort,
                meanDistanceKm = homePort.meanDistanceKm,
                homePortSource = homePort.source.name,
                pageUrl = vessel?.let { baseUrl + PagesController.vesselPath(it) },
                photoCount = photos.size,
                mainPhoto = photos.firstOrNull()?.let { PhotoDataOutput.from(baseUrl, vessel!!, it) },
                owner = portVessel.owner?.let { VesselOwnerDataOutput(it.name, it.group, it.sourceUrl) },
            )
        }
    }
}

data class VesselOwnerDataOutput(
    val name: String,
    val group: String?,
    val sourceUrl: String,
)

data class PortDetailsDataOutput(
    val port: PortDataOutput,
    val vessels: List<PortVesselDataOutput>,
    val auction: AuctionDataOutput?,
) {
    companion object {
        fun from(
            baseUrl: String,
            details: PortDetails,
        ) = PortDetailsDataOutput(
            port = PortDataOutput.from(details.port, details.vessels.size),
            vessels = details.vessels.map { PortVesselDataOutput.from(baseUrl, it) },
            auction = details.auction?.let { AuctionDataOutput.from(it) },
        )
    }
}

/** Sales declared at the fish auction of a port in its last year, compared with the year before (FranceAgriMer) */
data class AuctionDataOutput(
    /** e.g. Boulogne */
    val name: String,
    val year: Int,
    val quantityTonnes: Double,
    val valueThousandEuros: Double,
    val priceEurosPerKg: Double?,
    /** Quantity sold the year before, when known */
    val previousQuantityTonnes: Double?,
    /** The main species sold, by value */
    val species: List<AuctionSpeciesDataOutput>,
) {
    companion object {
        private const val MAX_SPECIES = 12

        fun from(auction: PortAuction): AuctionDataOutput? {
            val sales = auction.sales.filter { it.year == auction.year }
            val previousSales = auction.sales.filter { it.year == auction.year - 1 }.associateBy { it.species }
            val total = sales.find { it.species == null } ?: return null

            return AuctionDataOutput(
                name = auction.name,
                year = auction.year,
                quantityTonnes = total.quantityTonnes,
                valueThousandEuros = total.valueThousandEuros,
                priceEurosPerKg = total.priceEurosPerKg,
                previousQuantityTonnes = previousSales[null]?.quantityTonnes,
                species =
                    sales
                        .filter { it.species != null && it.quantityTonnes > 0 && !it.species.startsWith("Autres") }
                        .sortedByDescending { it.valueThousandEuros }
                        .take(MAX_SPECIES)
                        .map { sale ->
                            AuctionSpeciesDataOutput(
                                name = sale.species!!,
                                quantityTonnes = sale.quantityTonnes,
                                valueThousandEuros = sale.valueThousandEuros,
                                priceEurosPerKg = sale.priceEurosPerKg,
                                previousQuantityTonnes = previousSales[sale.species]?.quantityTonnes,
                                monthlyQuantitiesTonnes =
                                    auction.monthlySales[sale.species]
                                        ?.takeIf { it.size == 12 }
                                        ?.sortedBy { it.month }
                                        ?.map { it.quantityTonnes },
                            )
                        },
            )
        }
    }
}

data class AuctionSpeciesDataOutput(
    val name: String,
    val quantityTonnes: Double,
    val valueThousandEuros: Double,
    val priceEurosPerKg: Double?,
    val previousQuantityTonnes: Double?,
    /** Its sales in all French auctions by month, from January: its season; null for the less sold species */
    val monthlyQuantitiesTonnes: List<Double>?,
)
