package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.HomePort
import org.openvesselphotos.domain.entities.HomePortCandidate
import org.openvesselphotos.domain.entities.HomePortCoverage
import org.openvesselphotos.domain.entities.HomePortSearchResult
import org.openvesselphotos.domain.entities.HomePortSource
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
        // The home ports found on Global Fishing Watch are kept, unless the import gives one; the corrected ones and the
        // ones from returns to port always are
        jdbcTemplate.update("DELETE FROM vessel_home_ports WHERE source = 'IMPORT'")
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO vessel_home_ports
                (cfr, vessel_name, port_locode, observations_total, observations_at_port, mean_distance_km, imported_on,
                 source)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'IMPORT')
            ON CONFLICT (cfr) DO UPDATE SET
                vessel_name = excluded.vessel_name,
                port_locode = excluded.port_locode,
                observations_total = excluded.observations_total,
                observations_at_port = excluded.observations_at_port,
                mean_distance_km = excluded.mean_distance_km,
                imported_on = excluded.imported_on,
                source = 'IMPORT'
            WHERE vessel_home_ports.source NOT IN ('MANUAL', 'RETURNS')
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

    override fun findHomePortCandidates(): List<HomePortCandidate> =
        jdbcTemplate.query(
            """
            SELECT v.cfr, v.name, v.mmsi, v.call_sign, v.port, h.source FROM vessels v
            LEFT JOIN vessel_home_ports h ON h.cfr = v.cfr
            WHERE v.cfr LIKE 'FRA%' AND (h.cfr IS NULL OR h.source = 'REGISTER')
            ORDER BY v.id
            """.trimIndent(),
        ) { row, _ ->
            HomePortCandidate(
                cfr = row.getString("cfr"),
                name = row.getString("name"),
                mmsi = row.getString("mmsi"),
                callSign = row.getString("call_sign"),
                placeOfRegistration = row.getString("port"),
                source = row.getString("source")?.let { HomePortSource.valueOf(it) },
            )
        }

    override fun addHomePorts(homePorts: List<HomePort>) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO vessel_home_ports
                (cfr, vessel_name, port_locode, observations_total, observations_at_port, mean_distance_km, imported_on,
                 source)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (cfr) DO UPDATE SET
                vessel_name = excluded.vessel_name,
                port_locode = excluded.port_locode,
                observations_total = excluded.observations_total,
                observations_at_port = excluded.observations_at_port,
                mean_distance_km = excluded.mean_distance_km,
                imported_on = excluded.imported_on,
                source = excluded.source
            -- Only a home port from the place of registration is replaced: by a better one, or when found again
            WHERE vessel_home_ports.source = 'REGISTER'
            """.trimIndent(),
            homePorts.map {
                arrayOf<Any?>(
                    it.cfr,
                    it.vesselName,
                    it.portLocode,
                    it.observationsTotal,
                    it.observationsAtPort,
                    it.meanDistanceKm,
                    Date.valueOf(LocalDate.now()),
                    it.source.name,
                )
            },
        )
    }

    override fun replaceHomePortsFromPortReturns(
        ports: List<Port>,
        homePorts: List<HomePort>,
        importedOn: LocalDate,
    ) {
        // Only the unknown ports are created: the known ones keep their name and position
        jdbcTemplate.batchUpdate(
            "INSERT INTO ports (locode, name, country) VALUES (?, ?, ?) ON CONFLICT (locode) DO NOTHING",
            ports.map { arrayOf<Any>(it.locode, it.name, it.country) },
        )
        jdbcTemplate.update("DELETE FROM vessel_home_ports WHERE source = 'RETURNS'")
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO vessel_home_ports
                (cfr, vessel_name, port_locode, observations_total, observations_at_port, mean_distance_km, imported_on,
                 source)
            VALUES (?, coalesce(?, (SELECT max(name) FROM vessels WHERE cfr = ?)), ?, ?, ?, NULL, ?, 'RETURNS')
            ON CONFLICT (cfr) DO UPDATE SET
                vessel_name = coalesce(excluded.vessel_name, vessel_home_ports.vessel_name),
                port_locode = excluded.port_locode,
                observations_total = excluded.observations_total,
                observations_at_port = excluded.observations_at_port,
                mean_distance_km = NULL,
                imported_on = excluded.imported_on,
                source = 'RETURNS'
            WHERE vessel_home_ports.source <> 'MANUAL'
            """.trimIndent(),
            homePorts.map {
                arrayOf<Any?>(
                    it.cfr,
                    it.vesselName,
                    it.cfr,
                    it.portLocode,
                    it.observationsTotal,
                    it.observationsAtPort,
                    Date.valueOf(importedOn),
                )
            },
        )
    }

    override fun countHomePortsByPlaceOfRegistration(): Map<String, Map<String, Int>> =
        jdbcTemplate
            .queryForList(
                """
                SELECT v.port, h.port_locode, count(*) AS count FROM vessels v
                JOIN vessel_home_ports h ON h.cfr = v.cfr
                WHERE v.cfr LIKE 'FRA%' AND v.port IS NOT NULL AND h.source <> 'REGISTER'
                GROUP BY v.port, h.port_locode
                """.trimIndent(),
            ).groupBy({ it["port"] as String }, { it["port_locode"] as String to (it["count"] as Number).toInt() })
            .mapValues { (_, counts) -> counts.toMap() }

    override fun findLastHomePortsUpdate(): LocalDate? =
        jdbcTemplate.queryForObject("SELECT max(imported_on) FROM vessel_home_ports", Date::class.java)?.toLocalDate()

    override fun findCoverage(): HomePortCoverage {
        val bySource =
            jdbcTemplate
                .queryForList("SELECT source, count(*) AS count FROM vessel_home_ports GROUP BY source")
                .associate { it["source"] as String to (it["count"] as Number).toInt() }

        return jdbcTemplate.queryForObject(
            """
            SELECT count(*) AS register_vessels, count(h.cfr) AS with_home_port
            FROM vessels v LEFT JOIN vessel_home_ports h ON h.cfr = v.cfr
            WHERE v.cfr LIKE 'FRA%'
            """.trimIndent(),
        ) { row, _ ->
            HomePortCoverage(
                registerVessels = row.getInt("register_vessels"),
                withHomePort = row.getInt("with_home_port"),
                fromObservations = bySource[HomePortSource.IMPORT.name] ?: 0,
                fromPortVisits = bySource[HomePortSource.GFW.name] ?: 0,
                fromRegistration = bySource[HomePortSource.REGISTER.name] ?: 0,
                fromPortReturns = bySource[HomePortSource.RETURNS.name] ?: 0,
            )
        }!!
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

    override fun findHomePort(cfr: String): HomePort? =
        jdbcTemplate.query("SELECT * FROM vessel_home_ports WHERE cfr = ?", HOME_PORT_MAPPER, cfr).firstOrNull()

    override fun setManualHomePort(
        cfr: String,
        vesselName: String?,
        locode: String,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO vessel_home_ports
                (cfr, vessel_name, port_locode, observations_total, observations_at_port, mean_distance_km, imported_on,
                 source)
            VALUES (?, ?, ?, 0, 0, NULL, ?, 'MANUAL')
            ON CONFLICT (cfr) DO UPDATE SET
                vessel_name = coalesce(excluded.vessel_name, vessel_home_ports.vessel_name),
                port_locode = excluded.port_locode,
                observations_total = 0,
                observations_at_port = 0,
                mean_distance_km = NULL,
                imported_on = excluded.imported_on,
                source = 'MANUAL'
            """.trimIndent(),
            cfr,
            vesselName,
            locode,
            Date.valueOf(LocalDate.now()),
        )
    }

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
                    source = HomePortSource.valueOf(row.getString("source")),
                )
            }

        private fun ResultSet.getDoubleOrNull(column: String): Double? = getDouble(column).takeUnless { wasNull() }
    }
}
