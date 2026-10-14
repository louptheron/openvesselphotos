package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselKey
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
                    "INSERT INTO vessels (cfr, mmsi, name, registration, port) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                    batch.map { arrayOf(it.cfr, it.mmsi, it.name, it.registration, it.port) },
                )
            created += inserted.count { it > 0 }

            val existing = batch.filterIndexed { index, _ -> inserted[index] == 0 }
            if (existing.isNotEmpty()) {
                val results =
                    jdbcTemplate.batchUpdate(
                        """
                        UPDATE vessels
                        SET name = coalesce(name, ?), registration = coalesce(registration, ?), port = coalesce(port, ?)
                        WHERE (cfr = ? OR (CAST(? AS VARCHAR) IS NULL AND mmsi = ?))
                          AND ((name IS NULL AND CAST(? AS VARCHAR) IS NOT NULL)
                            OR (registration IS NULL AND CAST(? AS VARCHAR) IS NOT NULL)
                            OR (port IS NULL AND CAST(? AS VARCHAR) IS NOT NULL))
                        """.trimIndent(),
                        existing.map {
                            arrayOf(
                                it.name,
                                it.registration,
                                it.port,
                                it.cfr,
                                it.cfr,
                                it.mmsi,
                                it.name,
                                it.registration,
                                it.port,
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
