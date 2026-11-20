package org.openvesselphotos.domain.entities

/** Sales declared at a fish auction (halle à marée, criée) in a year, for a species or for all species. */
data class AuctionSpeciesSales(
    /** e.g. Boulogne */
    val auction: String,
    val year: Int,
    /** null for all species */
    val species: String?,
    val quantityTonnes: Double,
    val valueThousandEuros: Double,
    /** The port of the auction (LOCODE), when matched; only read by AuctionSalesRepository.findSpeciesSales */
    val portLocode: String? = null,
) {
    /** Average price, in € per kg */
    val priceEurosPerKg: Double? get() = if (quantityTonnes > 0) valueThousandEuros / quantityTonnes else null
}

/** Sales of a species in all French auctions in a month (1 to 12) */
data class SpeciesMonthlySales(
    val species: String,
    val year: Int,
    val month: Int,
    val quantityTonnes: Double,
)

/** The content of FranceAgriMer's yearly workbook "Ventes déclarées en halles à marée" */
data class AuctionSalesData(
    val sales: List<AuctionSpeciesSales>,
    val monthlySales: List<SpeciesMonthlySales>,
)

/** The auction of a port, with its sales of the last two years and the seasons of its species */
data class PortAuction(
    val name: String,
    val year: Int,
    val sales: List<AuctionSpeciesSales>,
    /** Sales of each species in France by month of [year], when known */
    val monthlySales: Map<String, List<SpeciesMonthlySales>>,
)

/** The sales of all species at the auction of a port, in its last year */
data class AuctionTotal(
    val portLocode: String,
    val auction: String,
    val year: Int,
    val quantityTonnes: Double,
    val valueThousandEuros: Double,
)

/** A species sold in the French auctions in its last year, compared with the year before (FranceAgriMer) */
data class SpeciesSales(
    /** e.g. Sole commune */
    val name: String,
    /** In the URL of its page, e.g. "sole-commune" */
    val slug: String,
    val year: Int,
    val quantityTonnes: Double,
    val valueThousandEuros: Double,
    val previousQuantityTonnes: Double?,
    val previousValueThousandEuros: Double?,
    /** Its sales at each auction in [year], the largest first */
    val auctions: List<AuctionSpeciesSales>,
    /** Its sales in all French auctions by month of [year], when known */
    val monthlySales: List<SpeciesMonthlySales>,
) {
    val priceEurosPerKg: Double? get() = if (quantityTonnes > 0) valueThousandEuros / quantityTonnes else null
}
