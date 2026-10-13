package org.openvesselphotos.infrastructure.api.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class RateLimiterUTests {
    private class MutableClock(
        var now: Instant,
    ) : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?) = this

        override fun instant() = now
    }

    @Test
    fun `tryConsume Should allow requests up to the limit, then reset after the window`() {
        val clock = MutableClock(Instant.parse("2026-10-13T10:00:00Z"))
        val rateLimiter = RateLimiter(clock)

        val decisions = (1..3).map { rateLimiter.tryConsume("ip", 2, Duration.ofMinutes(1)) }

        assertThat(decisions.map { it.isAllowed }).containsExactly(true, true, false)
        assertThat(decisions.map { it.remaining }).containsExactly(1, 0, 0)
        assertThat(decisions.last().resetInSeconds).isEqualTo(60)
        // Other clients are counted separately
        assertThat(rateLimiter.tryConsume("other-ip", 2, Duration.ofMinutes(1)).isAllowed).isTrue()

        clock.now = clock.now.plusSeconds(61)
        assertThat(rateLimiter.tryConsume("ip", 2, Duration.ofMinutes(1)).isAllowed).isTrue()
    }
}
