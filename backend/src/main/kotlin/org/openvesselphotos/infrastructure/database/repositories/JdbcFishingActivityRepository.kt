package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.HomePortCandidate
import org.openvesselphotos.domain.fishing_activity.FishingActivityRepository
import org.openvesselphotos.domain.fishing_activity.FishingCell
import org.openvesselphotos.domain.fishing_activity.HomePortCheck
import org.openvesselphotos.domain.fishing_activity.HomePortCheckResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.time.LocalDate

/** Plain SQL: the cells of a vessel are replaced at once, in a batch. */
@Repository
class JdbcFishingActivityRepository(
    private val jdbcTemplate: JdbcTemplate,
) : FishingActivityRepository {
    override fun findPortVessels(locode: String?): List<HomePortCandidate> =
        jdbcTemplate.query(
            """
            SELECT h.cfr, coalesce(v.name, h.vessel_name) AS name, v.mmsi, v.call_sign
            FROM vessel_home_ports h LEFT JOIN vessels v ON v.cfr = h.cfr
            WHERE CAST(? AS TEXT) IS NULL OR h.port_locode = CAST(? AS TEXT)
            ORDER BY h.cfr
            """.trimIndent(),
            { row, _ ->
                HomePortCandidate(
                    cfr = row.getString("cfr"),
                    name = row.getString("name"),
                    mmsi = row.getString("mmsi"),
                    callSign = row.getString("call_sign"),
                )
            },
            locode,
            locode,
        )

    override fun findImportedHomePorts(locode: String?): List<Pair<HomePortCandidate, String>> =
        jdbcTemplate.query(
            """
            SELECT h.cfr, coalesce(v.name, h.vessel_name) AS name, v.mmsi, v.call_sign, h.port_locode
            FROM vessel_home_ports h LEFT JOIN vessels v ON v.cfr = h.cfr
            WHERE h.source = 'IMPORT' AND (CAST(? AS TEXT) IS NULL OR h.port_locode = CAST(? AS TEXT))
            ORDER BY h.cfr
            """.trimIndent(),
            { row, _ ->
                HomePortCandidate(
                    cfr = row.getString("cfr"),
                    name = row.getString("name"),
                    mmsi = row.getString("mmsi"),
                    callSign = row.getString("call_sign"),
                ) to row.getString("port_locode")
            },
            locode,
            locode,
        )

    override fun saveHomePortCheck(check: HomePortCheck) {
        jdbcTemplate.update(
            """
            INSERT INTO home_port_checks (cfr, imported_locode, result, gfw_locode, gfw_port_name, visits_total,
                visits_at_gfw_port, visits_at_imported, distance_km, checked_on)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (cfr) DO UPDATE SET imported_locode = excluded.imported_locode, result = excluded.result,
                gfw_locode = excluded.gfw_locode, gfw_port_name = excluded.gfw_port_name,
                visits_total = excluded.visits_total, visits_at_gfw_port = excluded.visits_at_gfw_port,
                visits_at_imported = excluded.visits_at_imported, distance_km = excluded.distance_km,
                checked_on = excluded.checked_on
            """.trimIndent(),
            check.cfr,
            check.importedLocode,
            check.result.name,
            check.gfwLocode,
            check.gfwPortName?.take(MAX_NAME_LENGTH),
            check.visitsTotal,
            check.visitsAtGfwPort,
            check.visitsAtImported,
            check.distanceKm,
            Date.valueOf(check.checkedOn),
        )
    }

    override fun findHomePortChecks(): List<HomePortCheck> =
        jdbcTemplate.query(
            """
            SELECT c.*, coalesce(v.name, h.vessel_name) AS name, p.name AS imported_port_name
            FROM home_port_checks c
            LEFT JOIN vessels v ON v.cfr = c.cfr
            LEFT JOIN vessel_home_ports h ON h.cfr = c.cfr
            LEFT JOIN ports p ON p.locode = c.imported_locode
            ORDER BY c.cfr
            """.trimIndent(),
        ) { row, _ ->
            HomePortCheck(
                cfr = row.getString("cfr"),
                vesselName = row.getString("name"),
                importedLocode = row.getString("imported_locode"),
                importedPortName = row.getString("imported_port_name"),
                result = HomePortCheckResult.valueOf(row.getString("result")),
                gfwLocode = row.getString("gfw_locode"),
                gfwPortName = row.getString("gfw_port_name"),
                visitsTotal = row.getInt("visits_total"),
                visitsAtGfwPort = row.getInt("visits_at_gfw_port"),
                visitsAtImported = row.getInt("visits_at_imported"),
                distanceKm = row.getBigDecimal("distance_km")?.toDouble(),
                checkedOn = row.getDate("checked_on").toLocalDate(),
            )
        }

    override fun findVesselIds(since: LocalDate): Map<String, List<String>> =
        jdbcTemplate
            .query(
                "SELECT cfr, vessel_ids FROM gfw_vessels WHERE looked_up_on >= ?",
                { row, _ -> row.getString("cfr") to row.getString("vessel_ids").split(",").filter { it.isNotBlank() } },
                Date.valueOf(since),
            ).toMap()

    override fun saveVesselIds(
        cfr: String,
        vesselIds: List<String>,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO gfw_vessels (cfr, vessel_ids, looked_up_on) VALUES (?, ?, ?)
            ON CONFLICT (cfr) DO UPDATE SET vessel_ids = excluded.vessel_ids, looked_up_on = excluded.looked_up_on
            """.trimIndent(),
            cfr,
            vesselIds.joinToString(",").take(MAX_IDS_LENGTH),
            Date.valueOf(LocalDate.now()),
        )
    }

    override fun replaceCells(
        cfr: String,
        cells: List<FishingCell>,
    ) {
        jdbcTemplate.update("DELETE FROM vessel_fishing_cells WHERE cfr = ?", cfr)
        jdbcTemplate.batchUpdate(
            "INSERT INTO vessel_fishing_cells (cfr, latitude, longitude, hours) VALUES (?, ?, ?, ?)",
            cells.map { arrayOf<Any?>(cfr, it.latitude, it.longitude, it.hours) },
        )
    }

    override fun findPortCells(
        locode: String,
        gearCodes: List<String>?,
    ): List<FishingCell> =
        jdbcTemplate.query(
            """
            SELECT c.latitude, c.longitude, sum(c.hours) AS hours
            FROM vessel_fishing_cells c
            JOIN vessel_home_ports h ON h.cfr = c.cfr
            LEFT JOIN vessels v ON v.cfr = c.cfr
            WHERE h.port_locode = ? $GEAR_FILTER
            GROUP BY c.latitude, c.longitude
            """.trimIndent(),
            { row, _ -> FishingCell(row.getDouble("latitude"), row.getDouble("longitude"), row.getDouble("hours")) },
            locode,
            gearCodes?.joinToString(","),
            gearCodes?.joinToString(","),
        )

    override fun countTrackedPortVessels(
        locode: String,
        gearCodes: List<String>?,
    ): Int =
        jdbcTemplate.queryForObject(
            """
            SELECT count(DISTINCT c.cfr)
            FROM vessel_fishing_cells c
            JOIN vessel_home_ports h ON h.cfr = c.cfr
            LEFT JOIN vessels v ON v.cfr = c.cfr
            WHERE h.port_locode = ? $GEAR_FILTER
            """.trimIndent(),
            Int::class.java,
            locode,
            gearCodes?.joinToString(","),
            gearCodes?.joinToString(","),
        ) ?: 0

    companion object {
        /** The vessels whose main gear is one of the given ones (comma-separated), or all of them */
        private const val GEAR_FILTER =
            "AND (CAST(? AS TEXT) IS NULL OR v.main_gear = ANY(string_to_array(CAST(? AS TEXT), ',')))"
        private const val MAX_IDS_LENGTH = 2000
        private const val MAX_NAME_LENGTH = 200
    }
}
