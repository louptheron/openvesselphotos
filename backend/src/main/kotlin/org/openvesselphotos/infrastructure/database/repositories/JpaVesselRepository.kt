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

    override fun findRelatedWithApprovedPhotos(
        vessel: Vessel,
        limit: Int,
    ): List<Vessel> =
        dbVesselRepository
            .findRelatedWithApprovedPhotos(
                id = requireNotNull(vessel.id),
                port = vessel.port,
                flag = vessel.flag,
                mid = vessel.mmsi?.take(3),
                limit = limit,
            ).map { it.toVessel() }

    override fun findPageWithApprovedPhotos(
        offset: Int,
        limit: Int,
    ): List<Vessel> = dbVesselRepository.findPageWithApprovedPhotos(offset, limit).map { it.toVessel() }

    /** JDBC batches: a whole fleet register (tens of thousands of vessels) imports in seconds. */
    override fun importAll(vessels: List<Vessel>): VesselImportCounts {
        var created = 0
        var updated = 0
        val columns = IMPORTED_COLUMNS.joinToString(", ") { it.name }
        // Fills in the missing values, replaces the register's data, and only counts the vessels that changed
        val updateSql =
            """
            UPDATE vessels
            SET ${IMPORTED_COLUMNS.joinToString(", ") {
                if (it.isReplaced) {
                    "${it.name} = coalesce(CAST(? AS ${it.type}), ${it.name})"
                } else {
                    "${it.name} = coalesce(${it.name}, ?)"
                }
            }}
            WHERE (cfr = ? OR (CAST(? AS VARCHAR) IS NULL AND mmsi = ?))
              AND (${IMPORTED_COLUMNS.joinToString(" OR ") {
                if (it.isReplaced) {
                    "(CAST(? AS ${it.type}) IS NOT NULL AND ${it.name} IS DISTINCT FROM CAST(? AS ${it.type}))"
                } else {
                    "(${it.name} IS NULL AND CAST(? AS ${it.type}) IS NOT NULL)"
                }
            }})
            """.trimIndent()
        vessels.chunked(BATCH_SIZE).forEach { batch ->
            // Any unique key already taken (CFR or MMSI) means the vessel exists
            val inserted =
                jdbcTemplate.batchUpdate(
                    "INSERT INTO vessels (cfr, mmsi, $columns) " +
                        "VALUES (?, ?, ${IMPORTED_COLUMNS.joinToString(", ") { "?" }}) ON CONFLICT DO NOTHING",
                    batch.map {
                        arrayOf(
                            it.cfr,
                            it.mmsi,
                            *IMPORTED_COLUMNS
                                .map { column ->
                                    column.value(it)
                                }.toTypedArray(),
                        )
                    },
                )
            created += inserted.count { it > 0 }

            val existing = batch.filterIndexed { index, _ -> inserted[index] == 0 }
            if (existing.isNotEmpty()) {
                val results =
                    jdbcTemplate.batchUpdate(
                        updateSql,
                        existing.map { vessel ->
                            val values = IMPORTED_COLUMNS.map { it.value(vessel) }
                            val conditions =
                                IMPORTED_COLUMNS.flatMap {
                                    if (it.isReplaced) {
                                        listOf(
                                            it.value(vessel),
                                            it.value(vessel),
                                        )
                                    } else {
                                        listOf(it.value(vessel))
                                    }
                                }

                            (values + listOf(vessel.cfr, vessel.cfr, vessel.mmsi) + conditions).toTypedArray()
                        },
                    )
                updated += results.count { it > 0 }
            }

            // The day of this import, for the vessels it gives technical data to: not counted as an update
            val fromRegister = batch.filterNot { it.register.isEmpty }
            if (fromRegister.isNotEmpty()) {
                jdbcTemplate.batchUpdate(
                    "UPDATE vessels SET register_imported_on = CURRENT_DATE " +
                        "WHERE cfr = ? OR (CAST(? AS VARCHAR) IS NULL AND mmsi = ?)",
                    fromRegister.map { arrayOf(it.cfr, it.cfr, it.mmsi) },
                )
            }
        }

        return VesselImportCounts(created = created, updated = updated)
    }

    /** A column set by imports: filled in when missing, or replaced by the register's latest value. */
    private class ImportedColumn(
        val name: String,
        val type: String,
        val isReplaced: Boolean,
        val value: (Vessel) -> Any?,
    )

    companion object {
        const val BATCH_SIZE = 1000

        private val IMPORTED_COLUMNS =
            listOf(
                ImportedColumn("name", "VARCHAR", false) { it.name },
                ImportedColumn("registration", "VARCHAR", false) { it.registration },
                ImportedColumn("port", "VARCHAR", false) { it.port },
                ImportedColumn("vessel_type", "VARCHAR", true) { it.register.vesselType },
                ImportedColumn("year_built", "INTEGER", true) { it.register.yearBuilt },
                ImportedColumn("length_m", "NUMERIC", true) { it.register.lengthMeters?.toBigDecimal() },
                ImportedColumn("engine_power_kw", "NUMERIC", true) { it.register.enginePowerKw?.toBigDecimal() },
                ImportedColumn("tonnage_gt", "NUMERIC", true) { it.register.tonnageGt?.toBigDecimal() },
                ImportedColumn("main_gear", "VARCHAR", true) { it.register.mainGear },
                ImportedColumn(
                    "other_gears",
                    "VARCHAR",
                    true,
                ) {
                    it.register.otherGears
                        .joinToString(",")
                        .ifEmpty { null }
                },
                ImportedColumn("call_sign", "VARCHAR", true) { it.register.callSign },
            )
    }
}
