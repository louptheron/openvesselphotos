package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.HomePortCandidate
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.fishing_activity.FishingActivityClient
import org.openvesselphotos.domain.fishing_activity.FishingActivityRepository
import org.openvesselphotos.domain.fishing_activity.FishingCell
import org.openvesselphotos.domain.fishing_activity.FishingEvent
import org.openvesselphotos.domain.fishing_activity.VesselIdentity
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.floor

data class FishingActivitySyncStatus(
    val isRunning: Boolean,
    /** Vessels of the ports site */
    val total: Int,
    val done: Int,
    /** Vessels found on Global Fishing Watch */
    val found: Int,
    /** Vessels with fishing in the last 12 months */
    val withFishing: Int,
    val failed: Int,
    val errors: List<String>,
    val startedAt: ZonedDateTime?,
    val finishedAt: ZonedDateTime?,
)

/**
 * Where the vessels of the ports site fished over the last 12 months, for the map of each port: their fishing events on
 * Global Fishing Watch, summed by cell of [CELL_DEGREES]. Vessels are found once by their MMSI, call sign, then name, and
 * looked up again after [LOOKUP_DAYS]. Runs in the background, a vessel at a time; to run again every month or so.
 */
@UseCase
class SyncFishingActivity(
    private val fishingActivityRepository: FishingActivityRepository,
    private val fishingActivityClient: FishingActivityClient,
) {
    private val logger = LoggerFactory.getLogger(SyncFishingActivity::class.java)

    @Volatile
    private var current = FishingActivitySyncStatus(false, 0, 0, 0, 0, 0, emptyList(), null, null)

    fun status(): FishingActivitySyncStatus = current

    /** All the vessels of the ports site, or of this port only when given */
    @Synchronized
    fun start(locode: String? = null): FishingActivitySyncStatus {
        if (!fishingActivityClient.isEnabled) {
            throw BackendUsageException(
                BackendUsageErrorCode.INVALID_INPUT,
                "No Global Fishing Watch token: set OPENVESSELPHOTOS_GFW_TOKEN.",
            )
        }
        if (current.isRunning) {
            throw BackendUsageException(BackendUsageErrorCode.CONFLICT, "A synchronisation is already running.")
        }
        val vessels = fishingActivityRepository.findPortVessels(locode?.trim()?.uppercase())
        current =
            FishingActivitySyncStatus(
                isRunning = vessels.isNotEmpty(),
                total = vessels.size,
                done = 0,
                found = 0,
                withFishing = 0,
                failed = 0,
                errors = emptyList(),
                startedAt = ZonedDateTime.now(),
                finishedAt = if (vessels.isEmpty()) ZonedDateTime.now() else null,
            )
        if (vessels.isNotEmpty()) {
            Thread.ofVirtual().name("gfw-fishing-activity").start { run(vessels) }
        }

        return current
    }

    private fun run(vessels: List<HomePortCandidate>) {
        logger.info("Synchronising the fishing activity of ${vessels.size} vessels with Global Fishing Watch.")
        val to = LocalDate.now()
        val from = to.minusYears(1)
        val knownIds = fishingActivityRepository.findVesselIds(to.minusDays(LOOKUP_DAYS))
        vessels.forEach { vessel ->
            var status = current
            try {
                val ids =
                    knownIds[vessel.cfr] ?: fishingActivityClient
                        .findVesselIds(VesselIdentity(vessel.mmsi, vessel.callSign, vessel.name, FLAG))
                        .also { fishingActivityRepository.saveVesselIds(vessel.cfr, it) }
                if (ids.isNotEmpty()) {
                    status = status.copy(found = status.found + 1)
                    val cells = toCells(fishingActivityClient.findFishingEvents(ids, from, to))
                    fishingActivityRepository.replaceCells(vessel.cfr, cells)
                    if (cells.isNotEmpty()) {
                        status = status.copy(withFishing = status.withFishing + 1)
                    }
                }
                Thread.sleep(DELAY_BETWEEN_VESSELS_MS)
            } catch (e: Exception) {
                logger.warn("Could not synchronise the fishing activity of ${vessel.cfr}: ${e.message}")
                status =
                    status.copy(
                        failed = status.failed + 1,
                        errors = (status.errors + "${vessel.cfr}: ${e.message}").take(ImportPhotos.MAX_ERRORS),
                    )
            }
            current = status.copy(done = status.done + 1)
        }
        current = current.copy(isRunning = false, finishedAt = ZonedDateTime.now())
        logger.info("Fishing activity synchronised: $current")
    }

    companion object {
        private const val FLAG = "FRA"

        /** About 5 km: fine enough for a port's fishing grounds, small enough to store */
        const val CELL_DEGREES = 0.05

        /** Identifiers looked up less long ago are kept, the vessels not found included */
        const val LOOKUP_DAYS = 30L

        private const val DELAY_BETWEEN_VESSELS_MS = 300L

        /** The hours of fishing of the events summed by cell, each event in the cell of its position */
        fun toCells(events: List<FishingEvent>): List<FishingCell> =
            events
                .filter { it.hours > 0 }
                .groupBy { cellOf(it.latitude) to cellOf(it.longitude) }
                .map { (cell, cellEvents) ->
                    FishingCell(cell.first, cell.second, Math.round(cellEvents.sumOf { it.hours } * 100) / 100.0)
                }

        /** The centre of the cell of a coordinate, e.g. 47.67 → 47.675 */
        private fun cellOf(coordinate: Double): Double =
            Math.round((floor(coordinate / CELL_DEGREES) * CELL_DEGREES + CELL_DEGREES / 2) * 1000) / 1000.0
    }
}
