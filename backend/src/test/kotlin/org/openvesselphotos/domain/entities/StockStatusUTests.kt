package org.openvesselphotos.domain.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StockStatusUTests {
    private fun stock(
        region: FishingRegion,
        status: StockStatus,
        tonnes: Double,
    ) = FishStock("Sole commune (Solea solea), $region", region, status, tonnes)

    @Test
    fun `status Should be the one shared by the main stocks, ignoring the small ones`() {
        val sole =
            SpeciesStocks(
                listOf(
                    stock(FishingRegion.MNMEST, StockStatus.EFFONDRE, 544.0),
                    // Less than 5 % of the landings
                    stock(FishingRegion.MNMEST, StockStatus.BON_ETAT, 3.0),
                ),
            )

        assertThat(sole.status).isEqualTo(StockStatus.EFFONDRE)
    }

    @Test
    fun `status Should be null When the main stocks differ`() {
        val sole =
            SpeciesStocks(
                listOf(
                    stock(FishingRegion.MNMEST, StockStatus.EFFONDRE, 544.0),
                    stock(FishingRegion.GASCOGNE, StockStatus.RECONSTITUABLE, 2262.0),
                ),
            )

        assertThat(sole.status).isNull()
        assertThat(sole.inRegion(FishingRegion.GASCOGNE)?.status).isEqualTo(StockStatus.RECONSTITUABLE)
        assertThat(sole.inRegion(FishingRegion.MED)).isNull()
    }

    @Test
    fun `ofPort Should give the region of the waters off a port`() {
        // Boulogne-sur-Mer, Erquy, Le Guilvinec, Sète, Ajaccio
        assertThat(FishingRegion.ofPort(50.73, 1.60)).isEqualTo(FishingRegion.MNMEST)
        assertThat(FishingRegion.ofPort(48.63, -2.47)).isEqualTo(FishingRegion.MOMCOE)
        assertThat(FishingRegion.ofPort(47.79, -4.29)).isEqualTo(FishingRegion.GASCOGNE)
        assertThat(FishingRegion.ofPort(43.40, 3.70)).isEqualTo(FishingRegion.MED)
        assertThat(FishingRegion.ofPort(41.92, 8.74)).isEqualTo(FishingRegion.MED)
        // Saint-Jean-de-Luz: the Atlantic
        assertThat(FishingRegion.ofPort(43.39, -1.66)).isEqualTo(FishingRegion.GASCOGNE)
        assertThat(FishingRegion.ofPort(null, 1.0)).isNull()
    }
}
