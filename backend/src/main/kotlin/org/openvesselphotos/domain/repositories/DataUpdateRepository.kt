package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.DataUpdate
import java.time.LocalDate

interface DataUpdateRepository {
    fun add(update: DataUpdate)

    /** The latest first */
    fun findLatest(limit: Int): List<DataUpdate>

    /** The day of the latest update; null without update */
    fun findLastDay(): LocalDate?
}
