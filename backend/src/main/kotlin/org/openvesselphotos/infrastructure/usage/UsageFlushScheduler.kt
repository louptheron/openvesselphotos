package org.openvesselphotos.infrastructure.usage

import jakarta.annotation.PreDestroy
import org.openvesselphotos.domain.use_cases.public_api.RecordUsage
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Saves the usage counted in memory every minute, and when the server stops (e.g. on a deployment). */
@Component
class UsageFlushScheduler(
    private val recordUsage: RecordUsage,
) {
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    fun flush() {
        recordUsage.flush()
    }

    @PreDestroy
    fun flushOnShutdown() {
        recordUsage.flush()
    }
}
