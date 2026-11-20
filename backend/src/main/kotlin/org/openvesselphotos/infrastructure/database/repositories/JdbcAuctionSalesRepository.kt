package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.AuctionSpeciesSales
import org.openvesselphotos.domain.entities.AuctionTotal
import org.openvesselphotos.domain.entities.SpeciesMonthlySales
import org.openvesselphotos.domain.repositories.AuctionSalesRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/** Plain SQL: the sales are replaced at once, in batches. */
@Repository
class JdbcAuctionSalesRepository(
    private val jdbcTemplate: JdbcTemplate,
) : AuctionSalesRepository {
    override fun replaceAll(
        sales: List<AuctionSpeciesSales>,
        monthlySales: List<SpeciesMonthlySales>,
        portByAuction: Map<String, String>,
    ) {
        jdbcTemplate.update("DELETE FROM auction_sales")
        jdbcTemplate.update("DELETE FROM species_monthly_sales")
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO auction_sales (auction, port_locode, year, species, quantity_t, value_keur)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            sales.map {
                arrayOf<Any?>(
                    it.auction,
                    portByAuction[it.auction],
                    it.year,
                    it.species,
                    it.quantityTonnes,
                    it.valueThousandEuros,
                )
            },
        )
        jdbcTemplate.batchUpdate(
            "INSERT INTO species_monthly_sales (species, year, month, quantity_t) VALUES (?, ?, ?, ?)",
            monthlySales.map { arrayOf<Any?>(it.species, it.year, it.month, it.quantityTonnes) },
        )
    }

    override fun findByPort(locode: String): List<AuctionSpeciesSales> =
        jdbcTemplate.query(
            "SELECT * FROM auction_sales WHERE port_locode = ? ORDER BY auction, year, value_keur DESC",
            { row, _ ->
                AuctionSpeciesSales(
                    auction = row.getString("auction"),
                    year = row.getInt("year"),
                    species = row.getString("species"),
                    quantityTonnes = row.getDouble("quantity_t"),
                    valueThousandEuros = row.getDouble("value_keur"),
                )
            },
            locode,
        )

    override fun findSpeciesSales(): List<AuctionSpeciesSales> =
        jdbcTemplate.query(
            "SELECT * FROM auction_sales WHERE species IS NOT NULL ORDER BY year, quantity_t DESC",
        ) { row, _ ->
            AuctionSpeciesSales(
                auction = row.getString("auction"),
                year = row.getInt("year"),
                species = row.getString("species"),
                quantityTonnes = row.getDouble("quantity_t"),
                valueThousandEuros = row.getDouble("value_keur"),
                portLocode = row.getString("port_locode"),
            )
        }

    override fun findMonthlySales(year: Int): List<SpeciesMonthlySales> =
        jdbcTemplate.query(
            "SELECT * FROM species_monthly_sales WHERE year = ? ORDER BY species, month",
            { row, _ ->
                SpeciesMonthlySales(
                    species = row.getString("species"),
                    year = row.getInt("year"),
                    month = row.getInt("month"),
                    quantityTonnes = row.getDouble("quantity_t"),
                )
            },
            year,
        )

    override fun findTotals(): List<AuctionTotal> =
        jdbcTemplate.query(
            """
            SELECT DISTINCT ON (port_locode) port_locode, auction, year, quantity_t, value_keur
            FROM auction_sales
            WHERE species IS NULL AND port_locode IS NOT NULL
            ORDER BY port_locode, year DESC
            """.trimIndent(),
        ) { row, _ ->
            AuctionTotal(
                portLocode = row.getString("port_locode"),
                auction = row.getString("auction"),
                year = row.getInt("year"),
                quantityTonnes = row.getDouble("quantity_t"),
                valueThousandEuros = row.getDouble("value_keur"),
            )
        }
}
