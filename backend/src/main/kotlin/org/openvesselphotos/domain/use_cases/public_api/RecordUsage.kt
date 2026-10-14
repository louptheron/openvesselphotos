package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.UsageCount
import org.openvesselphotos.domain.entities.UsageKey
import org.openvesselphotos.domain.entities.UsageKind
import org.openvesselphotos.domain.repositories.UsageRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

/**
 * Counts the requests in memory, and adds them to the database at each [flush] (every minute): a request never waits
 * for a database write. Counts of the last minute are lost if the server stops abruptly.
 */
@UseCase
class RecordUsage(
    private val usageRepository: UsageRepository,
) {
    private val logger = LoggerFactory.getLogger(RecordUsage::class.java)
    private val counts = ConcurrentHashMap<UsageKey, LongAdder>()

    fun record(
        kind: UsageKind,
        source: String,
        apiKeyId: Int?,
    ) {
        val key = UsageKey(LocalDate.now(), kind, source, apiKeyId)
        val counter =
            counts[key]
                ?: if (counts.size < MAX_KEYS) {
                    counts.computeIfAbsent(key) { LongAdder() }
                } else {
                    counts.computeIfAbsent(key.copy(source = UsageKey.OTHER)) { LongAdder() }
                }
        counter.increment()
    }

    fun flush() {
        val today = LocalDate.now()
        val pending =
            counts.mapNotNull { (key, counter) ->
                val count = counter.sumThenReset()
                // Past days are not counted anymore
                if (key.day.isBefore(today)) {
                    counts.remove(key)
                }
                if (count > 0) UsageCount(key, count) else null
            }
        if (pending.isEmpty()) {
            return
        }
        try {
            usageRepository.add(pending)
        } catch (e: Exception) {
            logger.warn("Could not save ${pending.size} usage counts, kept for the next attempt: ${e.message}")
            pending.forEach { counts.computeIfAbsent(it.key) { LongAdder() }.add(it.count) }
        }
    }

    companion object {
        /** Distinct (day, kind, source, API key) counted between two flushes */
        const val MAX_KEYS = 2000
    }
}
