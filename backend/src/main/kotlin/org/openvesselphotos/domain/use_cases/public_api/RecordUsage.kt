package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.PageViewCount
import org.openvesselphotos.domain.entities.PageViewKey
import org.openvesselphotos.domain.entities.UsageCount
import org.openvesselphotos.domain.entities.UsageKey
import org.openvesselphotos.domain.entities.UsageKind
import org.openvesselphotos.domain.entities.Website
import org.openvesselphotos.domain.repositories.PageViewRepository
import org.openvesselphotos.domain.repositories.UsageRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

/**
 * Counts the requests and the page views in memory, and adds them to the database at each [flush] (every minute): a request never waits
 * for a database write. Counts of the last minute are lost if the server stops abruptly.
 */
@UseCase
class RecordUsage(
    private val usageRepository: UsageRepository,
    private val pageViewRepository: PageViewRepository,
) {
    private val logger = LoggerFactory.getLogger(RecordUsage::class.java)
    private val counts = ConcurrentHashMap<UsageKey, LongAdder>()
    private val pageViews = ConcurrentHashMap<PageViewKey, LongAdder>()

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

    /** A page shown on one of the websites, e.g. "/vessels/cfr/FRA000123456" */
    fun recordPageView(
        site: Website,
        path: String,
    ) {
        val key = PageViewKey(LocalDate.now(), site, path)
        val counter =
            pageViews[key]
                ?: if (pageViews.size < MAX_KEYS) {
                    pageViews.computeIfAbsent(key) { LongAdder() }
                } else {
                    pageViews.computeIfAbsent(key.copy(path = UsageKey.OTHER)) { LongAdder() }
                }
        counter.increment()
    }

    fun flush() {
        flush(counts, "usage counts", UsageKey::day, ::UsageCount, usageRepository::add)
        flush(pageViews, "page views", PageViewKey::day, ::PageViewCount, pageViewRepository::add)
    }

    private fun <K : Any, C> flush(
        counters: ConcurrentHashMap<K, LongAdder>,
        name: String,
        dayOf: (K) -> LocalDate,
        countOf: (K, Long) -> C,
        save: (List<C>) -> Unit,
    ) {
        val today = LocalDate.now()
        val pending =
            counters.mapNotNull { (key, counter) ->
                val count = counter.sumThenReset()
                // Past days are not counted anymore
                if (dayOf(key).isBefore(today)) {
                    counters.remove(key)
                }
                if (count > 0) key to count else null
            }
        if (pending.isEmpty()) {
            return
        }
        try {
            save(pending.map { (key, count) -> countOf(key, count) })
        } catch (e: Exception) {
            logger.warn("Could not save ${pending.size} $name, kept for the next attempt: ${e.message}")
            pending.forEach { (key, count) -> counters.computeIfAbsent(key) { LongAdder() }.add(count) }
        }
    }

    companion object {
        /** Distinct (day, kind, source, API key), and distinct (day, site, path), counted between two flushes */
        const val MAX_KEYS = 2000
    }
}
