package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.DataUpdate
import org.openvesselphotos.domain.entities.HomePortCoverage
import org.openvesselphotos.domain.entities.PortWithVesselCount
import org.openvesselphotos.domain.repositories.AuctionSalesRepository
import org.openvesselphotos.domain.repositories.DataUpdateRepository
import org.openvesselphotos.domain.repositories.PortRepository
import java.time.LocalDate

/** The ports with vessels based at them, by name. */
@UseCase
class GetPorts(
    private val portRepository: PortRepository,
    private val auctionSalesRepository: AuctionSalesRepository,
    private val dataUpdateRepository: DataUpdateRepository,
) {
    /** With the fish auction of each port, when it has one */
    fun execute(): List<PortWithVesselCount> {
        val auctionsByPort = auctionSalesRepository.findTotals().associateBy { it.portLocode }

        return withUniqueSlugs(
            portRepository.findAllWithVessels().map { it.copy(auction = auctionsByPort[it.port.locode]) },
        )
    }

    /** The day the data last changed, to date the pages */
    fun lastUpdate(): LocalDate? =
        listOfNotNull(dataUpdateRepository.findLastDay(), portRepository.findLastHomePortsUpdate()).maxOrNull()

    /** The latest updates of the data, the latest first, for the changelog */
    fun updates(): List<DataUpdate> = dataUpdateRepository.findLatest(MAX_UPDATES)

    fun coverage(): HomePortCoverage = portRepository.findCoverage()

    companion object {
        private const val MAX_UPDATES = 100

        /**
         * Ports of the same name (e.g. two "Saint-Pierre") get their LOCODE in their slug, e.g. "saint-pierre-frxyz",
         * except the one with the most vessels: so its URL does not change when another one comes.
         */
        fun withUniqueSlugs(ports: List<PortWithVesselCount>): List<PortWithVesselCount> {
            val keptByPlainSlug =
                ports
                    .groupBy { it.slug }
                    .mapValues { (_, samePorts) ->
                        samePorts.maxWith(compareBy({ it.vesselCount }, { it.port.locode }))
                    }

            return ports.map {
                if (keptByPlainSlug[it.slug] === it) it else it.copy(slug = "${it.slug}-${it.port.locode.lowercase()}")
            }
        }
    }
}
