package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.FishingRegion
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.entities.PortAuction
import org.openvesselphotos.domain.entities.PortDetails
import org.openvesselphotos.domain.entities.PortVessel
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.AuctionSalesRepository
import org.openvesselphotos.domain.repositories.PortRepository
import org.openvesselphotos.domain.repositories.StockAssessmentRepository
import org.openvesselphotos.domain.repositories.VesselRepository
import org.openvesselphotos.domain.use_cases.admin.ImportAuctionSales

/** A port and the vessels based at it, with their photos: the ones with photos first, then by name. */
@UseCase
class GetPort(
    private val portRepository: PortRepository,
    private val vesselRepository: VesselRepository,
    private val vesselPhotosLoader: VesselPhotosLoader,
    private val auctionSalesRepository: AuctionSalesRepository,
    private val stockAssessmentRepository: StockAssessmentRepository,
) {
    fun execute(locode: String): PortDetails {
        val port =
            portRepository.findByLocode(locode.trim().uppercase())
                ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No port $locode.")
        val homePorts = portRepository.findHomePorts(port.locode)
        val vesselsByCfr =
            vesselPhotosLoader
                .load(vesselRepository.findByCfrs(homePorts.map { it.cfr }))
                .associateBy { it.vessel.cfr }
        val ownersByCfr = portRepository.findOwners(homePorts.map { it.cfr }).associateBy { it.cfr }
        val vessels =
            homePorts
                .map { PortVessel(it, vesselsByCfr[it.cfr], ownersByCfr[it.cfr]) }
                .sortedWith(
                    compareBy<PortVessel> { it.vessel?.photos.isNullOrEmpty() }
                        .thenBy { (it.vessel?.vessel?.name ?: it.homePort.vesselName) ?: "￿" },
                )

        return PortDetails(port, vessels, findAuction(port))
    }

    /**
     * The auction of the port, with the seasons of the species sold there in its last year, and the status of the
     * stocks they are landed from in the waters of the port
     */
    private fun findAuction(port: Port): PortAuction? {
        val sales = auctionSalesRepository.findByPort(port.locode)
        val auction = sales.firstOrNull()?.auction ?: return null
        val auctionSales = sales.filter { it.auction == auction }
        val year = auctionSales.maxOf { it.year }
        // The two sheets of the workbook do not write the names the same way, e.g. "Rouget-Barbet" and "Rouget-barbet"
        val monthlySalesByName =
            auctionSalesRepository.findMonthlySales(year).groupBy { ImportAuctionSales.normalize(it.species) }
        val monthlySales =
            auctionSales
                .filter { it.year == year }
                .mapNotNull { it.species }
                .mapNotNull { species ->
                    monthlySalesByName[ImportAuctionSales.normalize(species)]?.let { species to it }
                }.toMap()

        val stockAssessment = stockAssessmentRepository.find()
        val region = FishingRegion.ofPort(port.latitude, port.longitude)
        val stocks =
            if (region == null) {
                emptyMap()
            } else {
                auctionSales
                    .filter { it.year == year }
                    .mapNotNull { it.species }
                    .mapNotNull { species ->
                        stockAssessment.bySpecies[ImportAuctionSales.normalize(species)]
                            ?.inRegion(region)
                            ?.let { species to it }
                    }.toMap()
            }

        return PortAuction(auction, year, auctionSales, monthlySales, stocks, stockAssessment)
    }
}
