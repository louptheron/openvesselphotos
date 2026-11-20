package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.AuctionSpeciesSales
import org.openvesselphotos.domain.entities.AuctionTotal
import org.openvesselphotos.domain.entities.SpeciesMonthlySales

interface AuctionSalesRepository {
    /** Replaces every sale with the given ones; [portByAuction] gives the port (LOCODE) of each auction, when matched */
    fun replaceAll(
        sales: List<AuctionSpeciesSales>,
        monthlySales: List<SpeciesMonthlySales>,
        portByAuction: Map<String, String>,
    )

    /** The sales of the auction of the port, all years */
    fun findByPort(locode: String): List<AuctionSpeciesSales>

    /** Monthly sales of all the species of the year */
    fun findMonthlySales(year: Int): List<SpeciesMonthlySales>

    /** The total sales of each auction matched to a port, in its last year */
    fun findTotals(): List<AuctionTotal>
}
