package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.AuctionSalesData
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.AuctionSalesRepository
import org.openvesselphotos.domain.repositories.PortRepository
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer

data class AuctionSalesImportResult(
    /** The latest year of the workbook */
    val year: Int,
    val auctions: Int,
    /** Auction to port, e.g. "Boulogne → Nord-Boulogne (FRNUO)" */
    val matched: List<String>,
    /** Auctions whose port is not known: their sales are kept, but not shown */
    val unmatched: List<String>,
    /** Species with monthly sales */
    val seasonalSpecies: Int,
)

/**
 * Replaces the sales of the fish auctions with the ones of FranceAgriMer's yearly workbook. Each auction is matched
 * to a port by its name (e.g. "Le Guilvinec" to "Guilvinec"), or by [ALIASES] when the auction is named after its
 * town or island.
 */
@UseCase
class ImportAuctionSales(
    private val auctionSalesRepository: AuctionSalesRepository,
    private val portRepository: PortRepository,
) {
    @Transactional
    fun execute(data: AuctionSalesData): AuctionSalesImportResult {
        if (data.sales.isEmpty()) {
            throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "No auction sales found in the file.")
        }
        val ports = portRepository.findAllWithVessels().map { it.port }
        val auctions = data.sales.map { it.auction }.distinct()
        val portByAuction = auctions.mapNotNull { auction -> matchPort(auction, ports)?.let { auction to it } }.toMap()
        auctionSalesRepository.replaceAll(data.sales, data.monthlySales, portByAuction.mapValues { it.value.locode })

        return AuctionSalesImportResult(
            year = data.sales.maxOf { it.year },
            auctions = auctions.size,
            matched = portByAuction.map { (auction, port) -> "$auction → ${port.name} (${port.locode})" },
            unmatched = auctions.filter { it !in portByAuction },
            seasonalSpecies =
                data.monthlySales
                    .map { it.species }
                    .distinct()
                    .size,
        )
    }

    companion object {
        /** Auctions named after their town or island, by normalized name: the LOCODE of their fishing port */
        val ALIASES =
            mapOf(
                // The fishing port of La Rochelle
                "larochelle" to "FRLRH-0015",
                // L'Herbaudière
                "noirmoutier" to "FRHID",
                // La Cotinière
                "oleron" to "FRLC5",
            )

        fun matchPort(
            auction: String,
            ports: List<Port>,
        ): Port? {
            val name = normalize(auction)
            ALIASES[name]?.let { locode -> return ports.find { it.locode == locode } }
            ports.find { normalize(it.name) == name }?.let { return it }
            // e.g. "Le Guilvinec" and "Guilvinec", "Boulogne" and "Nord-Boulogne": only when a single port matches
            val candidates =
                ports.filter {
                    val portName = normalize(it.name)
                    minOf(portName.length, name.length) >= MIN_PARTIAL_LENGTH &&
                        (portName.contains(name) || name.contains(portName))
                }

            return candidates.singleOrNull()
        }

        /** "Saint-Quay Portrieux" → "saintquayportrieux" */
        fun normalize(value: String): String =
            Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
                .lowercase()
                .replace(Regex("[^a-z]"), "")

        private const val MIN_PARTIAL_LENGTH = 5
    }
}
