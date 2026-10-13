package org.openvesselphotos.infrastructure.api.security

import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

data class RateLimitDecision(
    val isAllowed: Boolean,
    val limit: Int,
    val remaining: Int,
    /** Seconds until the window resets. */
    val resetInSeconds: Long,
)

/**
 * Fixed-window counters kept in memory: simple, and enough for one or a few containers (each counts separately).
 */
class RateLimiter(
    private val clock: Clock = Clock.systemUTC(),
) {
    private data class Window(
        val startMillis: Long,
        val count: Int,
    )

    private val windows = ConcurrentHashMap<String, Window>()

    fun tryConsume(
        key: String,
        limit: Int,
        window: Duration,
    ): RateLimitDecision {
        val now = clock.millis()
        val windowMillis = window.toMillis()
        val current =
            windows.compute(key) { _, existing ->
                if (existing == null || now - existing.startMillis >= windowMillis) {
                    Window(now, 1)
                } else {
                    existing.copy(count = existing.count + 1)
                }
            }!!
        cleanUpOccasionally(now)

        return RateLimitDecision(
            isAllowed = current.count <= limit,
            limit = limit,
            remaining = (limit - current.count).coerceAtLeast(0),
            resetInSeconds = ((current.startMillis + windowMillis - now) / 1000).coerceAtLeast(1),
        )
    }

    /** Drops the windows older than the longest one, so that memory stays bounded. */
    private fun cleanUpOccasionally(now: Long) {
        if (windows.size > CLEAN_UP_THRESHOLD) {
            windows.entries.removeIf { now - it.value.startMillis >= MAX_WINDOW.toMillis() }
        }
    }

    companion object {
        private const val CLEAN_UP_THRESHOLD = 10_000
        private val MAX_WINDOW: Duration = Duration.ofHours(1)
    }
}
