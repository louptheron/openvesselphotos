package org.openvesselphotos.domain.use_cases.public_api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.AuctionSpeciesSales
import org.openvesselphotos.domain.entities.FishStock
import org.openvesselphotos.domain.entities.FishingOpportunities
import org.openvesselphotos.domain.entities.FishingOpportunity
import org.openvesselphotos.domain.entities.FishingRegion
import org.openvesselphotos.domain.entities.SpeciesMonthlySales
import org.openvesselphotos.domain.entities.SpeciesQuotas
import org.openvesselphotos.domain.entities.SpeciesStocks
import org.openvesselphotos.domain.entities.StockAssessment
import org.openvesselphotos.domain.entities.StockStatus

class GetSpeciesUTests {
    @Test
    fun `summarize Should add up the sales of each species in its last year, the most valuable first`() {
        val species =
            GetSpecies.summarize(
                listOf(
                    AuctionSpeciesSales("Erquy", 2025, "Rouget-Barbet", 6.0, 50.0, "FRQUY"),
                    AuctionSpeciesSales("La Rochelle", 2025, "Rouget-barbet", 10.0, 70.0, "FRLRH"),
                    AuctionSpeciesSales("La Rochelle", 2024, "Rouget-barbet", 8.0, 60.0, "FRLRH"),
                    AuctionSpeciesSales("Erquy", 2025, "Coquille St-Jacques", 100.0, 300.0, "FRQUY"),
                    AuctionSpeciesSales("Erquy", 2025, "Autres espèces", 500.0, 900.0, "FRQUY"),
                    // Not sold in the last year
                    AuctionSpeciesSales("Erquy", 2024, "Sole", 1.0, 20.0, "FRQUY"),
                ),
                listOf(
                    SpeciesMonthlySales("Rouget-Barbet", 2025, 2, 3.0),
                    SpeciesMonthlySales("Rouget-Barbet", 2025, 1, 2.0),
                    SpeciesMonthlySales("Rouget-Barbet", 2024, 1, 9.0),
                ),
            )

        assertThat(species.map { it.slug }).containsExactly("coquille-st-jacques", "rouget-barbet")
        val mullet = species[1]
        // Named as where it sold the most
        assertThat(mullet.name).isEqualTo("Rouget-barbet")
        assertThat(mullet.year).isEqualTo(2025)
        assertThat(mullet.quantityTonnes).isEqualTo(16.0)
        assertThat(mullet.valueThousandEuros).isEqualTo(120.0)
        assertThat(mullet.priceEurosPerKg).isEqualTo(7.5)
        assertThat(mullet.previousQuantityTonnes).isEqualTo(8.0)
        assertThat(mullet.auctions.map { it.auction }).containsExactly("La Rochelle", "Erquy")
        assertThat(mullet.monthlySales.map { it.month }).containsExactly(1, 2)
        assertThat(species[0].previousQuantityTonnes).isNull()
    }

    @Test
    fun `summarize Should return no species without sales`() {
        assertThat(GetSpecies.summarize(emptyList(), emptyList())).isEmpty()
    }

    @Test
    fun `summarize Should give each species the stocks it is landed from, by its normalized name`() {
        val mulletStocks =
            SpeciesStocks(listOf(FishStock("Rouget de roche", FishingRegion.MNMEST, StockStatus.SURPECHE, 674.0)))
        val species =
            GetSpecies.summarize(
                listOf(
                    AuctionSpeciesSales("Erquy", 2025, "Rouget-Barbet", 6.0, 50.0, "FRQUY"),
                    AuctionSpeciesSales("Erquy", 2025, "Maigre", 1.0, 20.0, "FRQUY"),
                ),
                emptyList(),
                StockAssessment(2025, "https://example.org", mapOf("rougetbarbet" to mulletStocks)),
            )

        assertThat(species.first { it.slug == "rouget-barbet" }.stocks).isEqualTo(mulletStocks)
        // Not assessed
        assertThat(species.first { it.slug == "maigre" }.stocks).isNull()
    }

    @Test
    fun `summarize Should give each species the TACs it is fished under, by its normalized name`() {
        val soleQuotas =
            SpeciesQuotas(
                listOf(FishingOpportunity("SOL/07D.", "Manche Est (7d)", FishingRegion.MNMEST, 1275.0, 669.0)),
            )
        val species =
            GetSpecies.summarize(
                listOf(
                    AuctionSpeciesSales("Erquy", 2025, "Sole", 6.0, 50.0, "FRQUY"),
                    AuctionSpeciesSales("Erquy", 2025, "Bar", 1.0, 20.0, "FRQUY"),
                ),
                emptyList(),
                fishingOpportunities = FishingOpportunities(2026, "https://example.org", mapOf("sole" to soleQuotas)),
            )

        assertThat(species.first { it.slug == "sole" }.quotas).isEqualTo(soleQuotas)
        // No TAC
        assertThat(species.first { it.slug == "bar" }.quotas).isNull()
    }
}
