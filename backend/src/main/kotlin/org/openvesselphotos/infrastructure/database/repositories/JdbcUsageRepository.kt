package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.UsageCount
import org.openvesselphotos.domain.entities.UsageKey
import org.openvesselphotos.domain.entities.UsageKind
import org.openvesselphotos.domain.repositories.UsageRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.time.LocalDate

/** Plain SQL: one upsert per count, sent in a single batch. `api_key_id` is 0 without a key (it is in the primary key). */
@Repository
class JdbcUsageRepository(
    private val jdbcTemplate: JdbcTemplate,
) : UsageRepository {
    override fun add(counts: List<UsageCount>) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO usage_counts (day, kind, source, api_key_id, count) VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (day, kind, source, api_key_id) DO UPDATE SET count = usage_counts.count + excluded.count
            """.trimIndent(),
            counts.map {
                arrayOf<Any>(Date.valueOf(it.key.day), it.key.kind.name, it.key.source, it.key.apiKeyId ?: 0, it.count)
            },
        )
    }

    override fun findSince(day: LocalDate): List<UsageCount> =
        jdbcTemplate.query(
            "SELECT day, kind, source, api_key_id, count FROM usage_counts WHERE day >= ?",
            { row, _ ->
                UsageCount(
                    key =
                        UsageKey(
                            day = row.getDate("day").toLocalDate(),
                            kind = UsageKind.valueOf(row.getString("kind")),
                            source = row.getString("source"),
                            apiKeyId = row.getInt("api_key_id").takeIf { it != 0 },
                        ),
                    count = row.getLong("count"),
                )
            },
            Date.valueOf(day),
        )
}
