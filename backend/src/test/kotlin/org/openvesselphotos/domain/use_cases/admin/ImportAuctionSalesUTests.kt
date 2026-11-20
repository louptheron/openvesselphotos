package org.openvesselphotos.domain.use_cases.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.Port

class ImportAuctionSalesUTests {
    private val ports =
        listOf(
            Port("FRGVC", "Guilvinec", "FR"),
            Port("FRNUO", "Nord-Boulogne", "FR"),
            Port("FRSQ2", "Saint-Quay-Portrieux", "FR"),
            Port("FRLC5", "La Cotinière", "FR"),
            Port("FRDUU", "Le Douhet (Saint-Georges-d'Oléron)", "FR"),
            Port("FRHTR", "Le Château-D'Oléron", "FR"),
        )

    @Test
    fun `matchPort Should find the port of an auction by its name, accents and spacing aside`() {
        assertThat(ImportAuctionSales.matchPort("Saint-Quay Portrieux", ports)?.locode).isEqualTo("FRSQ2")
        assertThat(ImportAuctionSales.matchPort("Le Guilvinec", ports)?.locode).isEqualTo("FRGVC")
        assertThat(ImportAuctionSales.matchPort("Boulogne", ports)?.locode).isEqualTo("FRNUO")
    }

    @Test
    fun `matchPort Should use the aliases of the auctions named after their island, and never guess`() {
        // Two ports of Oléron match its name: the alias tells the one of the auction
        assertThat(ImportAuctionSales.matchPort("Oléron", ports)?.locode).isEqualTo("FRLC5")
        assertThat(ImportAuctionSales.matchPort("Arcachon", ports)).isNull()
        // Without its port, Oléron is not matched to another port of the island
        assertThat(ImportAuctionSales.matchPort("Oléron", ports.filter { it.locode != "FRLC5" })).isNull()
    }
}
