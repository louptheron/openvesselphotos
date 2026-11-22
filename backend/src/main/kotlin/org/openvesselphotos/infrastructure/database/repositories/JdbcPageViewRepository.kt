package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.PageViewCount
import org.openvesselphotos.domain.entities.PageViewKey
import org.openvesselphotos.domain.entities.Website
import org.openvesselphotos.domain.repositories.PageViewRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.time.LocalDate

/** Plain SQL, as [JdbcUsageRepository]: one upsert per count, sent in a single batch. */
@Repository
class JdbcPageViewRepository(
    private val jdbcTemplate: JdbcTemplate,
) : PageViewRepository {
    override fun add(counts: List<PageViewCount>) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO page_views (day, site, path, count) VALUES (?, ?, ?, ?)
            ON CONFLICT (day, site, path) DO UPDATE SET count = page_views.count + excluded.count
            """.trimIndent(),
            counts.map { arrayOf<Any>(Date.valueOf(it.key.day), it.key.site.name, it.key.path, it.count) },
        )
    }

    override fun findSince(day: LocalDate): List<PageViewCount> =
        jdbcTemplate.query(
            "SELECT day, site, path, count FROM page_views WHERE day >= ?",
            { row, _ ->
                PageViewCount(
                    key =
                        PageViewKey(
                            day = row.getDate("day").toLocalDate(),
                            site = Website.valueOf(row.getString("site")),
                            path = row.getString("path"),
                        ),
                    count = row.getLong("count"),
                )
            },
            Date.valueOf(day),
        )
}
