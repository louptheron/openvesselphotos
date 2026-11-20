package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.PortWithVesselCount
import org.openvesselphotos.domain.repositories.AuctionSalesRepository
import org.openvesselphotos.domain.repositories.PortRepository

/** The ports with vessels based at them, by name. */
@UseCase
class GetPorts(
    private val portRepository: PortRepository,
    private val auctionSalesRepository: AuctionSalesRepository,
) {
    /** With the fish auction of each port, when it has one */
    fun execute(): List<PortWithVesselCount> {
        val auctionsByPort = auctionSalesRepository.findTotals().associateBy { it.portLocode }

        return portRepository.findAllWithVessels().map { it.copy(auction = auctionsByPort[it.port.locode]) }
    }
}
