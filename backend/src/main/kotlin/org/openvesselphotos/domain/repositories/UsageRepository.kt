package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.UsageCount
import java.time.LocalDate

interface UsageRepository {
    /** Adds the counts to those already stored for the same day, kind, source and API key. */
    fun add(counts: List<UsageCount>)

    fun findSince(day: LocalDate): List<UsageCount>
}
