package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.AuctionSpeciesSales
import org.openvesselphotos.domain.entities.SpeciesMonthlySales
import org.openvesselphotos.domain.entities.SpeciesSales
import org.openvesselphotos.domain.entities.StockAssessment
import org.openvesselphotos.domain.entities.slugOf
import org.openvesselphotos.domain.repositories.AuctionSalesRepository
import org.openvesselphotos.domain.repositories.StockAssessmentRepository
import org.openvesselphotos.domain.use_cases.admin.ImportAuctionSales

/**
 * The species sold in the French auctions in the last year (FranceAgriMer), the most valuable first, with the status
 * of the stocks they are landed from (Ifremer).
 */
@UseCase
class GetSpecies(
    private val auctionSalesRepository: AuctionSalesRepository,
    private val stockAssessmentRepository: StockAssessmentRepository,
) {
    fun execute(): List<SpeciesSales> {
        val sales = auctionSalesRepository.findSpeciesSales()
        val year = sales.maxOfOrNull { it.year } ?: return emptyList()

        return summarize(sales, auctionSalesRepository.findMonthlySales(year), stockAssessmentRepository.find())
    }

    /** Ifremer's assessment of the stocks, for its edition and URL: to credit it */
    fun stockAssessment(): StockAssessment = stockAssessmentRepository.find()

    companion object {
        /**
         * The sales of each species in the last year of [sales], by auction. The auctions do not all write a species
         * the same way (e.g. "Rouget-Barbet" and "Rouget-barbet"): it is named as where it sold the most. "Autres
         * espèces…" is not a species.
         */
        fun summarize(
            sales: List<AuctionSpeciesSales>,
            monthlySales: List<SpeciesMonthlySales>,
            stockAssessment: StockAssessment = StockAssessment.NONE,
        ): List<SpeciesSales> {
            val year = sales.maxOfOrNull { it.year } ?: return emptyList()
            val monthlyByName = monthlySales.groupBy { ImportAuctionSales.normalize(it.species) }

            return sales
                .filter { it.species != null && !it.species.startsWith("autre", ignoreCase = true) }
                .groupBy { ImportAuctionSales.normalize(it.species!!) }
                .filterKeys { it.isNotEmpty() }
                .mapNotNull { (key, speciesSales) ->
                    val ofYear = speciesSales.filter { it.year == year }.sortedByDescending { it.quantityTonnes }
                    val previous = speciesSales.filter { it.year == year - 1 }
                    val name = ofYear.firstOrNull()?.species ?: return@mapNotNull null

                    SpeciesSales(
                        name = name,
                        slug = slugOf(name),
                        year = year,
                        quantityTonnes = ofYear.sumOf { it.quantityTonnes },
                        valueThousandEuros = ofYear.sumOf { it.valueThousandEuros },
                        previousQuantityTonnes = previous.takeIf { it.isNotEmpty() }?.sumOf { it.quantityTonnes },
                        previousValueThousandEuros =
                            previous.takeIf { it.isNotEmpty() }?.sumOf { it.valueThousandEuros },
                        auctions = ofYear,
                        monthlySales = monthlyByName[key].orEmpty().filter { it.year == year }.sortedBy { it.month },
                        stocks = stockAssessment.bySpecies[key],
                    )
                }.distinctBy { it.slug }
                .sortedByDescending { it.valueThousandEuros }
        }
    }
}
