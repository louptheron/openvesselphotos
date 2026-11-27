package org.openvesselphotos.infrastructure.api.admin

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openvesselphotos.TestWorkbooks
import org.openvesselphotos.domain.entities.AuctionSpeciesSales
import org.openvesselphotos.domain.exceptions.BackendUsageException

class AuctionSalesWorkbookParserUTests {
    @Test
    fun `parse Should read the sales of each auction by species, for both years`() {
        val data = AuctionSalesWorkbookParser.parse(TestWorkbooks.auctionSales())

        assertThat(data.sales).hasSize(12)
        assertThat(data.sales).contains(
            AuctionSpeciesSales("Erquy", 2025, null, 120.0, 400.0),
            AuctionSpeciesSales("Erquy", 2024, "Bar", 10.0, 150.0),
            AuctionSpeciesSales("La Rochelle", 2025, "Rouget-Barbet", 6.0, 50.0),
        )
        assertThat(data.sales.map { it.auction }.distinct()).containsExactly("Erquy", "La Rochelle")
    }

    @Test
    fun `parse Should read the monthly sales of the species sheets, an empty month being nothing sold`() {
        val data = AuctionSalesWorkbookParser.parse(TestWorkbooks.auctionSales())

        val bar2025 = data.monthlySales.filter { it.species == "Bar" && it.year == 2025 }.sortedBy { it.month }
        assertThat(bar2025.map { it.quantityTonnes })
            .containsExactly(2.0, 4.0, 0.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0)
        assertThat(data.monthlySales.filter { it.year == 2024 }).hasSize(12)
    }

    @Test
    fun `parse Should reject other files`() {
        assertThatThrownBy { AuctionSalesWorkbookParser.parse("not a workbook".toByteArray()) }
            .isInstanceOf(BackendUsageException::class.java)
        assertThatThrownBy {
            AuctionSalesWorkbookParser.parse(TestWorkbooks.workbook(listOf("Feuil1" to mapOf(1 to mapOf("A" to "x")))))
        }.isInstanceOf(BackendUsageException::class.java)
            .hasMessageContaining("Ventes déclarées en halles à marée")
    }
}
