package org.openvesselphotos.infrastructure.ifremer

import org.openvesselphotos.domain.entities.FishStock
import org.openvesselphotos.domain.entities.FishingRegion
import org.openvesselphotos.domain.entities.SpeciesStocks
import org.openvesselphotos.domain.entities.StockAssessment
import org.openvesselphotos.domain.entities.StockStatus
import org.openvesselphotos.domain.repositories.StockAssessmentRepository
import org.openvesselphotos.domain.use_cases.admin.ImportAuctionSales
import org.springframework.stereotype.Repository

/**
 * Ifremer publishes its assessment once a year, as a report (no API, no data file): its tables are copied by hand
 * into [PATH], read once.
 */
@Repository
class CsvStockAssessmentRepository : StockAssessmentRepository {
    private val assessment by lazy {
        val text =
            javaClass.getResourceAsStream(PATH)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("No $PATH")

        parse(text)
    }

    override fun find(): StockAssessment = assessment

    companion object {
        const val PATH = "/ifremer/stock-status.csv"

        /**
         * "# key: value" comments give the edition and the URL of the report; then a header and
         * "species;region;stock;status;tonnes" rows.
         */
        fun parse(text: String): StockAssessment {
            val lines = text.lines().filter { it.isNotBlank() }
            val metadata =
                lines
                    .filter { it.startsWith("#") }
                    .mapNotNull { Regex("^#\\s*(edition|url):\\s*(\\S+)").find(it) }
                    .associate { it.groupValues[1] to it.groupValues[2] }
            val stocks =
                lines
                    .filterNot { it.startsWith("#") }
                    .drop(1)
                    .mapIndexed { index, line ->
                        val cells = line.split(';')
                        require(cells.size == 5) { "Line ${index + 1} of $PATH: 5 cells expected, not ${cells.size}" }
                        cells[0].trim() to
                            FishStock(
                                name = cells[2].trim(),
                                region = FishingRegion.valueOf(cells[1].trim()),
                                status = StockStatus.valueOf(cells[3].trim()),
                                tonnes = cells[4].trim().toDouble(),
                            )
                    }

            return StockAssessment(
                edition = metadata["edition"]?.toInt() ?: error("No edition in $PATH"),
                url = metadata["url"] ?: error("No url in $PATH"),
                bySpecies =
                    stocks
                        .groupBy({ ImportAuctionSales.normalize(it.first) }, { it.second })
                        .mapValues { (_, it) -> SpeciesStocks(it.sortedByDescending { stock -> stock.tonnes }) },
            )
        }
    }
}
