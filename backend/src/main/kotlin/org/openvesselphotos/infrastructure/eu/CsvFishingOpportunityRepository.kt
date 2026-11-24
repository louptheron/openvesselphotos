package org.openvesselphotos.infrastructure.eu

import org.openvesselphotos.domain.entities.FishingOpportunities
import org.openvesselphotos.domain.entities.FishingOpportunity
import org.openvesselphotos.domain.entities.FishingRegion
import org.openvesselphotos.domain.entities.SpeciesQuotas
import org.openvesselphotos.domain.repositories.FishingOpportunityRepository
import org.openvesselphotos.domain.use_cases.admin.ImportAuctionSales
import org.springframework.stereotype.Repository

/**
 * The Council fixes the TACs and quotas once a year, in a regulation (tables in a PDF, no data file): the ones of the
 * species Ifremer assesses are copied by hand into [PATH], read once.
 */
@Repository
class CsvFishingOpportunityRepository : FishingOpportunityRepository {
    private val opportunities by lazy {
        val text =
            javaClass.getResourceAsStream(PATH)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("No $PATH")

        parse(text)
    }

    override fun find(): FishingOpportunities = opportunities

    companion object {
        const val PATH = "/eu/fishing-opportunities.csv"

        /**
         * "# key: value" comments give the year and the URL of the regulation; then a header and
         * "species;region;tac_code;area;tac_tonnes;france_tonnes" rows.
         */
        fun parse(text: String): FishingOpportunities {
            val lines = text.lines().filter { it.isNotBlank() }
            val metadata =
                lines
                    .filter { it.startsWith("#") }
                    .mapNotNull { Regex("^#\\s*(year|url):\\s*(\\S+)").find(it) }
                    .associate { it.groupValues[1] to it.groupValues[2] }
            val quotas =
                lines
                    .filterNot { it.startsWith("#") }
                    .drop(1)
                    .mapIndexed { index, line ->
                        val cells = line.split(';').map { it.trim() }
                        require(cells.size == 6) { "Line ${index + 1} of $PATH: 6 cells expected, not ${cells.size}" }
                        cells[0] to
                            FishingOpportunity(
                                tacCode = cells[2],
                                area = cells[3],
                                region = FishingRegion.valueOf(cells[1]),
                                tacTonnes = cells[4].takeIf { it.isNotEmpty() }?.toDouble(),
                                franceTonnes = cells[5].toDouble(),
                            )
                    }

            return FishingOpportunities(
                year = metadata["year"]?.toInt() ?: error("No year in $PATH"),
                url = metadata["url"] ?: error("No url in $PATH"),
                bySpecies =
                    quotas
                        .groupBy({ ImportAuctionSales.normalize(it.first) }, { it.second })
                        .mapValues { (_, it) -> SpeciesQuotas(it.sortedByDescending { quota -> quota.franceTonnes }) },
            )
        }
    }
}
