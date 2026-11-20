package org.openvesselphotos.domain.use_cases.public_api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.AuctionSpeciesSales
import org.openvesselphotos.domain.entities.SpeciesMonthlySales

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
}
