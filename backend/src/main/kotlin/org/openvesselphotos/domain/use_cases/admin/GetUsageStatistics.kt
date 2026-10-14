package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.ApiKeyStatus
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.UsageCount
import org.openvesselphotos.domain.entities.UsageKey
import org.openvesselphotos.domain.entities.UsageKind
import org.openvesselphotos.domain.repositories.ApiKeyRepository
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.UsageRepository
import org.openvesselphotos.domain.repositories.VesselRepository
import java.time.LocalDate
import java.time.ZonedDateTime

data class DailyUsage(
    val day: LocalDate,
    val imageViews: Long,
    val apiCalls: Long,
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
    val lastUsedAt: ZonedDateTime?,
)

data class UsageStatistics(
    val days: Int,
    val vesselCount: Int,
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
)

/** Usage of the photos and of the API over the last days, for the admin's statistics page. */
@UseCase
class GetUsageStatistics(
    private val usageRepository: UsageRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
) {
    fun execute(days: Int): UsageStatistics {
        val period = days.coerceIn(1, MAX_DAYS)
        val firstDay = LocalDate.now().minusDays(period - 1L)
        val counts = usageRepository.findSince(firstDay)
        val images = counts.filter { it.key.kind == UsageKind.IMAGE }
        val calls = counts.filter { it.key.kind == UsageKind.API }

        val callsByKey =
            calls
                .filter { it.key.apiKeyId != null }
                .groupBy { requireNotNull(it.key.apiKeyId) }
                .mapValues { (_, keyCounts) -> keyCounts.sumOf { it.count } }
        val keys = apiKeyRepository.findByIds(callsByKey.keys).associateBy { requireNotNull(it.id) }
        val elsewhere = images.filter { it.key.source !in LOCAL_SOURCES }

        return UsageStatistics(
            days = period,
            vesselCount = vesselRepository.count(),
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
                (0 until period).map { offset ->
                    val day = firstDay.plusDays(offset.toLong())
                    DailyUsage(
                        day = day,
                        imageViews = images.filter { it.key.day == day }.sumOf { it.count },
                        apiCalls = calls.filter { it.key.day == day }.sumOf { it.count },
                    )
                },
            imageSources = topSources(images),
            apiSources = topSources(calls),
            apiKeys =
                callsByKey
                    .mapNotNull { (id, count) ->
                        keys[id]?.let { ApiKeyUsage(id, it.name, it.organisation, count, it.lastUsedAt) }
                    }.sortedByDescending { it.calls },
        )
    }

    private fun topSources(counts: List<UsageCount>): List<SourceUsage> =
        counts
            .groupBy { it.key.source }
            .map { (source, sourceCounts) -> SourceUsage(source, sourceCounts.sumOf { it.count }) }
            .sortedByDescending { it.count }
            .take(MAX_SOURCES)

    companion object {
        const val MAX_DAYS = 365
        const val MAX_SOURCES = 30
        private val LOCAL_SOURCES = setOf(UsageKey.WEBSITE, UsageKey.DIRECT)
    }
}
