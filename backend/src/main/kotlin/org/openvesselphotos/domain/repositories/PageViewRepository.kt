package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.PageViewCount
import java.time.LocalDate

interface PageViewRepository {
    /** Adds the counts to those already stored for the same day, site and path. */
    fun add(counts: List<PageViewCount>)

    fun findSince(day: LocalDate): List<PageViewCount>
}
