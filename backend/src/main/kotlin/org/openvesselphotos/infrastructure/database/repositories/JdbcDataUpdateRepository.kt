package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.DataSource
import org.openvesselphotos.domain.entities.DataUpdate
import org.openvesselphotos.domain.repositories.DataUpdateRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.time.LocalDate

@Repository
class JdbcDataUpdateRepository(
    private val jdbcTemplate: JdbcTemplate,
) : DataUpdateRepository {
    override fun add(update: DataUpdate) {
        jdbcTemplate.update(
            "INSERT INTO data_updates (source, updated_on, summary) VALUES (?, ?, ?)",
            update.source.name,
            Date.valueOf(update.updatedOn),
            update.summary.take(MAX_SUMMARY_LENGTH),
        )
    }

    override fun findLatest(limit: Int): List<DataUpdate> =
        jdbcTemplate.query(
            "SELECT * FROM data_updates ORDER BY updated_on DESC, id DESC LIMIT ?",
            { row, _ ->
                DataUpdate(
                    source = DataSource.valueOf(row.getString("source")),
                    updatedOn = row.getDate("updated_on").toLocalDate(),
                    summary = row.getString("summary"),
                )
            },
            limit,
        )

    override fun findLastDay(): LocalDate? =
        jdbcTemplate.queryForObject("SELECT max(updated_on) FROM data_updates", Date::class.java)?.toLocalDate()

    companion object {
        private const val MAX_SUMMARY_LENGTH = 300
    }
}
