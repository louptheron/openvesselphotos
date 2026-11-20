package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.HomePort
import org.openvesselphotos.domain.entities.HomePortSearchResult
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.entities.PortWithVesselCount
import org.openvesselphotos.domain.entities.VesselOwner
import org.openvesselphotos.domain.repositories.PortRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate

/** Plain SQL: the home ports are replaced at once, in batches. */
@Repository
class JdbcPortRepository(
    private val jdbcTemplate: JdbcTemplate,
) : PortRepository {
    override fun replaceHomePorts(
        ports: List<Port>,
        homePorts: List<HomePort>,
        importedOn: LocalDate,
    ) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO ports (locode, name, country) VALUES (?, ?, ?)
            ON CONFLICT (locode) DO UPDATE SET name = excluded.name, country = excluded.country
            """.trimIndent(),
            ports.map { arrayOf<Any>(it.locode, it.name, it.country) },
        )
        jdbcTemplate.update("DELETE FROM vessel_home_ports")
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO vessel_home_ports
                (cfr, vessel_name, port_locode, observations_total, observations_at_port, mean_distance_km, imported_on)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            homePorts.map {
                arrayOf<Any?>(
                    it.cfr,
                    it.vesselName,
                    it.portLocode,
                    it.observationsTotal,
                    it.observationsAtPort,
                    it.meanDistanceKm,
                    Date.valueOf(importedOn),
                )
            },
        )
    }

    override fun savePositions(ports: List<Port>): Int =
        jdbcTemplate
            .batchUpdate(
                "UPDATE ports SET latitude = ?, longitude = ? WHERE locode = ?",
                ports.map { arrayOf<Any?>(it.latitude, it.longitude, it.locode) },
            ).sum()

    override fun findAllWithVessels(): List<PortWithVesselCount> {
        // The main gear comes from the EU fleet register, for the vessels known by Open Vessel Photos
        val countsByGear = mutableMapOf<String, MutableMap<String, Int>>()
        jdbcTemplate.query(
            """
            SELECT h.port_locode, v.main_gear, count(*) AS vessel_count
            FROM vessel_home_ports h JOIN vessels v ON v.cfr = h.cfr
            WHERE v.main_gear IS NOT NULL
            GROUP BY h.port_locode, v.main_gear
            """.trimIndent(),
        ) { row ->
            countsByGear.getOrPut(row.getString("port_locode")) { sortedMapOf() }[row.getString("main_gear")] =
                row.getInt("vessel_count")
        }

        return jdbcTemplate.query(
            """
            SELECT p.*, count(*) AS vessel_count
            FROM ports p JOIN vessel_home_ports h ON h.port_locode = p.locode
            GROUP BY p.locode
            ORDER BY p.name
            """.trimIndent(),
        ) { row, _ ->
            val port = toPort(row)
            PortWithVesselCount(port, row.getInt("vessel_count"), countsByGear[port.locode].orEmpty())
        }
    }

    override fun findByLocode(locode: String): Port? =
        jdbcTemplate.query("SELECT * FROM ports WHERE locode = ?", { row, _ -> toPort(row) }, locode).firstOrNull()

    override fun findHomePorts(locode: String): List<HomePort> =
        jdbcTemplate.query(
            "SELECT * FROM vessel_home_ports WHERE port_locode = ? ORDER BY vessel_name NULLS LAST, cfr",
            HOME_PORT_MAPPER,
            locode,
        )

    override fun replaceOwners(owners: List<VesselOwner>) {
        jdbcTemplate.update("DELETE FROM vessel_owners")
        jdbcTemplate.batchUpdate(
            "INSERT INTO vessel_owners (cfr, owner, owner_group, source_url) VALUES (?, ?, ?, ?)",
            owners.map { arrayOf<Any?>(it.cfr, it.name, it.group, it.sourceUrl) },
        )
    }

    override fun findOwners(cfrs: Collection<String>): List<VesselOwner> {
        if (cfrs.isEmpty()) {
            return emptyList()
        }

        return jdbcTemplate.query(
            "SELECT * FROM vessel_owners WHERE cfr IN (${cfrs.joinToString(",") { "?" }})",
            { row, _ ->
                VesselOwner(
                    cfr = row.getString("cfr"),
                    name = row.getString("owner"),
                    group = row.getString("owner_group"),
                    sourceUrl = row.getString("source_url"),
                )
            },
            *cfrs.toTypedArray(),
        )
    }

    override fun searchVessels(
        query: String,
        limit: Int,
    ): List<HomePortSearchResult> {
        val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

        // The name of the register when Open Vessel Photos knows the vessel, else the one of the observations
        return jdbcTemplate.query(
            """
            SELECT h.cfr, coalesce(v.name, h.vessel_name) AS name, p.locode, p.name AS port_name
            FROM vessel_home_ports h
            JOIN ports p ON p.locode = h.port_locode
            LEFT JOIN vessels v ON v.cfr = h.cfr
            WHERE coalesce(v.name, h.vessel_name) ILIKE '%' || ? || '%' OR h.cfr LIKE upper(?) || '%'
            ORDER BY coalesce(v.name, h.vessel_name) ILIKE ? || '%' DESC, coalesce(v.name, h.vessel_name), h.cfr
            LIMIT ?
            """.trimIndent(),
            { row, _ ->
                HomePortSearchResult(
                    cfr = row.getString("cfr"),
                    name = row.getString("name"),
                    portLocode = row.getString("locode"),
                    portName = row.getString("port_name"),
                )
            },
            escaped,
            escaped,
            escaped,
            limit,
        )
    }

    private fun toPort(row: ResultSet) =
        Port(
            locode = row.getString("locode"),
            name = row.getString("name"),
            country = row.getString("country"),
            latitude = row.getDoubleOrNull("latitude"),
            longitude = row.getDoubleOrNull("longitude"),
        )

    companion object {
        private val HOME_PORT_MAPPER =
            RowMapper { row, _ ->
                HomePort(
                    cfr = row.getString("cfr"),
                    vesselName = row.getString("vessel_name"),
                    portLocode = row.getString("port_locode"),
                    observationsTotal = row.getInt("observations_total"),
                    observationsAtPort = row.getInt("observations_at_port"),
                    meanDistanceKm = row.getDoubleOrNull("mean_distance_km"),
                )
            }

        private fun ResultSet.getDoubleOrNull(column: String): Double? = getDouble(column).takeUnless { wasNull() }
    }
}
