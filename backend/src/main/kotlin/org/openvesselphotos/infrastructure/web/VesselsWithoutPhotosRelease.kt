package org.openvesselphotos.infrastructure.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * How many pages of vessels without photo search engines may index today: [perDay] more each day since [since], up to
 * one sitemap file. A
 * few at a time, to follow in Search Console whether such pages get indexed before releasing thousands.
 */
@Component
class VesselsWithoutPhotosRelease(
    @param:Value("\${openvesselphotos.vessels-without-photos.since}")
    private val since: String,
    @param:Value("\${openvesselphotos.vessels-without-photos.per-day}")
    private val perDay: Int,
) {
    fun limit(today: LocalDate = LocalDate.now()): Int {
        val start = since.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) } ?: return 0
        val days = ChronoUnit.DAYS.between(start, today) + 1

        // One sitemap file at most
        return (days * perDay).coerceIn(0, CrawlersController.VESSELS_PER_SITEMAP.toLong()).toInt()
    }
}
