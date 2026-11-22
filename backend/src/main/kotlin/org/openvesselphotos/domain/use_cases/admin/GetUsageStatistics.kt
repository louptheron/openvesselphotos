package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.ApiKeyStatus
import org.openvesselphotos.domain.entities.PageViewCount
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.UsageCount
import org.openvesselphotos.domain.entities.UsageKey
import org.openvesselphotos.domain.entities.UsageKind
import org.openvesselphotos.domain.entities.Website
import org.openvesselphotos.domain.repositories.ApiKeyRepository
import org.openvesselphotos.domain.repositories.PageViewRepository
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.UsageRepository
import org.openvesselphotos.domain.repositories.VesselRepository
import java.time.LocalDate
import java.time.ZonedDateTime

data class DailyUsage(
    val day: LocalDate,
    val imageViews: Long,
    val apiCalls: Long,
    /** Vessels created that day, by a first photo or an import */
    val vesselsAdded: Int,
)

data class SourceUsage(
    /** [UsageKey.WEBSITE], [UsageKey.DIRECT], [UsageKey.OTHER] or the host of a site */
    val source: String,
    val count: Long,
)

data class ApiKeyUsage(
    val id: Int,
    val name: String,
    val organisation: String?,
    val calls: Long,
    /** Photos downloaded with the key */
    val imageViews: Long,
    val lastUsedAt: ZonedDateTime?,
)

data class SiteDailyUsage(
    val day: LocalDate,
    val pageViews: Long,
    val imageViews: Long,
    val apiCalls: Long,
)

data class PageUsage(
    /** The path of the page, e.g. "/vessels/cfr/FRA000123456", or [UsageKey.OTHER] */
    val path: String,
    val count: Long,
)

/** The usage of one website by its own pages */
data class SiteUsage(
    val site: Website,
    val pageViews: Long,
    /** Photos shown by the pages of the site */
    val imageViews: Long,
    /** API calls made by the pages of the site */
    val apiCalls: Long,
    /** One entry per day, oldest first, days without usage included */
    val daily: List<SiteDailyUsage>,
    val topPages: List<PageUsage>,
)

data class UsageStatistics(
    val days: Int,
    val vesselCount: Int,
    /** Vessels created during the period */
    val vesselsAdded: Int,
    val photoCount: Int,
    val imageViews: Long,
    /** Image views on other sites than this one */
    val imageViewsElsewhere: Long,
    /** Other sites showing the photos */
    val siteCount: Int,
    val apiCalls: Long,
    val apiCallsWithKey: Long,
    /** Active keys used during the period */
    val activeApiKeys: Int,
    val activeApiKeysTotal: Int,
    /** One entry per day, oldest first, days without usage included */
    val daily: List<DailyUsage>,
    val imageSources: List<SourceUsage>,
    val apiSources: List<SourceUsage>,
    val apiKeys: List<ApiKeyUsage>,
    /** One entry per [Website] */
    val sites: List<SiteUsage>,
)

/** Usage of the photos and of the API over the last days, for the admin's statistics page. */
@UseCase
class GetUsageStatistics(
    private val usageRepository: UsageRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
    private val pageViewRepository: PageViewRepository,
) {
    fun execute(days: Int): UsageStatistics {
        val period = days.coerceIn(1, MAX_DAYS)
        val firstDay = LocalDate.now().minusDays(period - 1L)
        val counts = usageRepository.findSince(firstDay)
        val vesselsByDay = vesselRepository.countCreatedByDay(firstDay)
        val images = counts.filter { it.key.kind == UsageKind.IMAGE }
        val calls = counts.filter { it.key.kind == UsageKind.API }

        val callsByKey = countByKey(calls)
        val imagesByKey = countByKey(images)
        val keys =
            apiKeyRepository.findByIds(callsByKey.keys + imagesByKey.keys).associateBy { requireNotNull(it.id) }
        val elsewhere = images.filter { it.key.source !in LOCAL_SOURCES }
        val pageViews = pageViewRepository.findSince(firstDay)
        val periodDays = (0 until period).map { firstDay.plusDays(it.toLong()) }

        return UsageStatistics(
            days = period,
            vesselCount = vesselRepository.count(),
            vesselsAdded = vesselsByDay.values.sum(),
            photoCount = photoRepository.countByStatus(PhotoStatus.APPROVED),
            imageViews = images.sumOf { it.count },
            imageViewsElsewhere = elsewhere.sumOf { it.count },
            siteCount =
                elsewhere
                    .map { it.key.source }
                    .filter { it != UsageKey.OTHER }
                    .distinct()
                    .size,
            apiCalls = calls.sumOf { it.count },
            apiCallsWithKey = callsByKey.values.sum(),
            activeApiKeys = keys.values.count { it.status == ApiKeyStatus.ACTIVE },
            activeApiKeysTotal = apiKeyRepository.findByStatus(ApiKeyStatus.ACTIVE).size,
            daily =
                periodDays.map { day ->
                    DailyUsage(
                        day = day,
                        imageViews = images.filter { it.key.day == day }.sumOf { it.count },
                        apiCalls = calls.filter { it.key.day == day }.sumOf { it.count },
                        vesselsAdded = vesselsByDay[day] ?: 0,
                    )
                },
            imageSources = topSources(images),
            apiSources = topSources(calls),
            apiKeys =
                keys
                    .map { (id, key) ->
                        ApiKeyUsage(
                            id = id,
                            name = key.name,
                            organisation = key.organisation,
                            calls = callsByKey[id] ?: 0,
                            imageViews = imagesByKey[id] ?: 0,
                            lastUsedAt = key.lastUsedAt,
                        )
                    }.sortedByDescending { it.calls + it.imageViews },
            sites =
                Website.entries.map { site ->
                    siteUsage(
                        site,
                        periodDays,
                        pageViews.filter { it.key.site == site },
                        images.filter { it.key.source == site.usageSource },
                        calls.filter { it.key.source == site.usageSource },
                    )
                },
        )
    }

    private fun siteUsage(
        site: Website,
        days: List<LocalDate>,
        pageViews: List<PageViewCount>,
        images: List<UsageCount>,
        calls: List<UsageCount>,
    ): SiteUsage =
        SiteUsage(
            site = site,
            pageViews = pageViews.sumOf { it.count },
            imageViews = images.sumOf { it.count },
            apiCalls = calls.sumOf { it.count },
            daily =
                days.map { day ->
                    SiteDailyUsage(
                        day = day,
                        pageViews = pageViews.filter { it.key.day == day }.sumOf { it.count },
                        imageViews = images.filter { it.key.day == day }.sumOf { it.count },
                        apiCalls = calls.filter { it.key.day == day }.sumOf { it.count },
                    )
                },
            topPages =
                pageViews
                    .groupBy { it.key.path }
                    .map { (path, pathCounts) -> PageUsage(path, pathCounts.sumOf { it.count }) }
                    .sortedByDescending { it.count }
                    .take(MAX_SOURCES),
        )

    private fun countByKey(counts: List<UsageCount>): Map<Int, Long> =
        counts
            .filter { it.key.apiKeyId != null }
            .groupBy { requireNotNull(it.key.apiKeyId) }
            .mapValues { (_, keyCounts) -> keyCounts.sumOf { it.count } }

    private fun topSources(counts: List<UsageCount>): List<SourceUsage> =
        counts
            .groupBy { it.key.source }
            .map { (source, sourceCounts) -> SourceUsage(source, sourceCounts.sumOf { it.count }) }
            .sortedByDescending { it.count }
            .take(MAX_SOURCES)

    companion object {
        const val MAX_DAYS = 365
        const val MAX_SOURCES = 30
        private val LOCAL_SOURCES = setOf(UsageKey.WEBSITE, UsageKey.PORTS_WEBSITE, UsageKey.DIRECT)
    }
}
