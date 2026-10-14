package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.repositories.PortVesselCounts
import org.openvesselphotos.domain.repositories.VesselCoverageCounts
import org.openvesselphotos.domain.repositories.VesselImportCounts
import org.openvesselphotos.domain.repositories.VesselRepository
import org.openvesselphotos.infrastructure.database.entities.VesselEntity
import org.openvesselphotos.infrastructure.database.repositories.interfaces.DBVesselRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class JpaVesselRepository(
    private val dbVesselRepository: DBVesselRepository,
    private val jdbcTemplate: JdbcTemplate,
) : VesselRepository {
    override fun findById(id: Int): Vessel? = dbVesselRepository.findById(id).orElse(null)?.toVessel()

    override fun findByKey(key: VesselKey): Vessel? =
        when (key.type) {
            KeyType.CFR -> dbVesselRepository.findByCfr(key.value)
            KeyType.MMSI -> dbVesselRepository.findByMmsi(key.value)
        }?.toVessel()

    override fun findByIds(ids: Collection<Int>): List<Vessel> =
        dbVesselRepository.findAllById(ids).map { it.toVessel() }

    override fun search(
        query: String,
        limit: Int,
    ): List<Vessel> {
        val escapedQuery = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val keyPrefix = VesselKey.normalize(escapedQuery)

        return dbVesselRepository.search(escapedQuery, keyPrefix, limit).map { it.toVessel() }
    }

    override fun findRecentlyPhotographed(limit: Int): List<Vessel> =
        dbVesselRepository.findRecentlyPhotographed(limit).map { it.toVessel() }

    override fun findAllWithApprovedPhotos(): List<Vessel> =
        dbVesselRepository.findAllWithApprovedPhotos().map {
            it.toVessel()
        }

    override fun save(vessel: Vessel): Vessel {
        val saved = dbVesselRepository.saveAndFlush(VesselEntity.fromVessel(vessel))

        // Reload to get database defaults (created_at)
        return findById(requireNotNull(saved.id)) ?: saved.toVessel()
    }

    override fun count(): Int = dbVesselRepository.count().toInt()

    override fun countCreatedByDay(day: LocalDate): Map<LocalDate, Int> =
        jdbcTemplate
            .query(
                "SELECT created_at::date AS day, count(*) AS count FROM vessels WHERE created_at >= ? GROUP BY 1",
                { row, _ -> row.getDate("day").toLocalDate() to row.getInt("count") },
                java.sql.Date.valueOf(day),
            ).toMap()

    override fun countWithApprovedPhotos(): Int = dbVesselRepository.countWithApprovedPhotos().toInt()

    override fun countCoverage(): VesselCoverageCounts =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FILTER (WHERE cfr IS NOT NULL) AS register_vessels,
                   count(*) FILTER (WHERE cfr IS NOT NULL AND photographed) AS register_photographed,
                   count(*) FILTER (WHERE photographed) AS photographed_vessels
            FROM (
                SELECT cfr, EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED') AS photographed
                FROM vessels v
            ) counted
            """.trimIndent(),
        ) { row, _ ->
            VesselCoverageCounts(
                registerVessels = row.getInt("register_vessels"),
                registerPhotographed = row.getInt("register_photographed"),
                photographedVessels = row.getInt("photographed_vessels"),
            )
        }!!

    override fun countByPort(code: String?): List<PortVesselCounts> =
        jdbcTemplate.query(
            """
            SELECT port_code,
                   mode() WITHIN GROUP (ORDER BY port) AS name,
                   count(*) AS vessels,
                   count(*) FILTER (WHERE photographed) AS photographed
            FROM (
                SELECT port_code, port,
                       EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED') AS photographed
                FROM vessels v
                WHERE port_code IS NOT NULL AND (CAST(? AS VARCHAR) IS NULL OR port_code = ?)
            ) counted
            GROUP BY port_code
            """.trimIndent(),
            { row, _ ->
                PortVesselCounts(
                    code = row.getString("port_code"),
                    name = row.getString("name"),
                    vessels = row.getInt("vessels"),
                    photographed = row.getInt("photographed"),
                )
            },
            code,
            code,
        )

    override fun findByPortCode(
        code: String,
        isPhotographed: Boolean,
        offset: Int,
        limit: Int,
    ): List<Vessel> = dbVesselRepository.findByPortCode(code, isPhotographed, offset, limit).map { it.toVessel() }

    override fun findPageWithApprovedPhotos(
        offset: Int,
        limit: Int,
    ): List<Vessel> = dbVesselRepository.findPageWithApprovedPhotos(offset, limit).map { it.toVessel() }

    /** JDBC batches: a whole fleet register (tens of thousands of vessels) imports in seconds. */
    override fun importAll(vessels: List<Vessel>): VesselImportCounts {
        var created = 0
        var updated = 0
        vessels.chunked(BATCH_SIZE).forEach { batch ->
            // Any unique key already taken (CFR or MMSI) means the vessel exists
            val inserted =
                jdbcTemplate.batchUpdate(
                    "INSERT INTO vessels (cfr, mmsi, name, registration, port, port_code) VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT DO NOTHING",
                    batch.map { arrayOf(it.cfr, it.mmsi, it.name, it.registration, it.port, it.portCode) },
                )
            created += inserted.count { it > 0 }

            val existing = batch.filterIndexed { index, _ -> inserted[index] == 0 }
            if (existing.isNotEmpty()) {
                val results =
                    jdbcTemplate.batchUpdate(
                        """
                        UPDATE vessels
                        SET name = coalesce(name, ?), registration = coalesce(registration, ?), port = coalesce(port, ?),
                            port_code = coalesce(port_code, ?)
                        WHERE (cfr = ? OR (CAST(? AS VARCHAR) IS NULL AND mmsi = ?))
                          AND ((name IS NULL AND CAST(? AS VARCHAR) IS NOT NULL)
                            OR (registration IS NULL AND CAST(? AS VARCHAR) IS NOT NULL)
                            OR (port IS NULL AND CAST(? AS VARCHAR) IS NOT NULL)
                            OR (port_code IS NULL AND CAST(? AS VARCHAR) IS NOT NULL))
                        """.trimIndent(),
                        existing.map {
                            arrayOf(
                                it.name,
                                it.registration,
                                it.port,
                                it.portCode,
                                it.cfr,
                                it.cfr,
                                it.mmsi,
                                it.name,
                                it.registration,
                                it.port,
                                it.portCode,
                            )
                        },
                    )
                updated += results.count { it > 0 }
            }
        }

        return VesselImportCounts(created = created, updated = updated)
    }

    companion object {
        const val BATCH_SIZE = 1000
    }
}
