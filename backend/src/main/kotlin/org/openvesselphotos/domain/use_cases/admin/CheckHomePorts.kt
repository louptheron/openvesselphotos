package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.HomePortCandidate
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.fishing_activity.FishingActivityClient
import org.openvesselphotos.domain.fishing_activity.FishingActivityRepository
import org.openvesselphotos.domain.fishing_activity.HomePortCheck
import org.openvesselphotos.domain.fishing_activity.HomePortCheckResult
import org.openvesselphotos.domain.fishing_activity.PortVisit
import org.openvesselphotos.domain.fishing_activity.VesselIdentity
import org.openvesselphotos.domain.repositories.PortRepository
import org.openvesselphotos.domain.use_cases.admin.FindHomePortsFromPortVisits.Companion.FLAG
import org.openvesselphotos.domain.use_cases.admin.FindHomePortsFromPortVisits.Companion.distanceKm
import org.openvesselphotos.domain.use_cases.admin.FindHomePortsFromPortVisits.Companion.mostVisitedPort
import org.openvesselphotos.domain.use_cases.admin.FindHomePortsFromPortVisits.Companion.portOnTheMap
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZonedDateTime

data class HomePortsCheckStatus(
    val isRunning: Boolean,
    /** Vessels with an imported home port */
    val total: Int,
    val done: Int,
    val countsByResult: Map<HomePortCheckResult, Int>,
    val failed: Int,
    val errors: List<String>,
    val startedAt: ZonedDateTime?,
    val finishedAt: ZonedDateTime?,
)

/**
 * Double checks the imported home ports against the port visits of the vessels on Global Fishing Watch: the port each
 * visited most in the last year, as in [FindHomePortsFromPortVisits], compared to its imported home port. Changes no
 * home port: the admin reviews the vessels whose port disagrees ([HomePortCheckResult.DISAGREES]). Runs in the
 * background, of a port only when given; only the vessels with AIS can be checked.
 */
@UseCase
class CheckHomePorts(
    private val portRepository: PortRepository,
    private val fishingActivityRepository: FishingActivityRepository,
    private val fishingActivityClient: FishingActivityClient,
) {
    private val logger = LoggerFactory.getLogger(CheckHomePorts::class.java)

    @Volatile
    private var current = HomePortsCheckStatus(false, 0, 0, emptyMap(), 0, emptyList(), null, null)

    fun status(): HomePortsCheckStatus = current

    fun results(): List<HomePortCheck> = fishingActivityRepository.findHomePortChecks()

    @Synchronized
    fun start(locode: String? = null): HomePortsCheckStatus {
        if (!fishingActivityClient.isEnabled) {
            throw BackendUsageException(
                BackendUsageErrorCode.INVALID_INPUT,
                "No Global Fishing Watch token: set OPENVESSELPHOTOS_GFW_TOKEN.",
            )
        }
        if (current.isRunning) {
            throw BackendUsageException(BackendUsageErrorCode.CONFLICT, "A check of the home ports is already running.")
        }
        val vessels = fishingActivityRepository.findImportedHomePorts(locode?.trim()?.uppercase())
        val ports =
            portRepository.findAllWithVessels().map { it.port }.filter { it.latitude != null && it.longitude != null }
        current =
            HomePortsCheckStatus(
                isRunning = vessels.isNotEmpty(),
                total = vessels.size,
                done = 0,
                countsByResult = emptyMap(),
                failed = 0,
                errors = emptyList(),
                startedAt = ZonedDateTime.now(),
                finishedAt = if (vessels.isEmpty()) ZonedDateTime.now() else null,
            )
        if (vessels.isNotEmpty()) {
            Thread.ofVirtual().name("gfw-home-ports-check").start { run(vessels, ports) }
        }

        return current
    }

    private fun run(
        vessels: List<Pair<HomePortCandidate, String>>,
        ports: List<Port>,
    ) {
        logger.info("Checking the home ports of ${vessels.size} vessels with Global Fishing Watch.")
        val to = LocalDate.now()
        val from = to.minusYears(1)
        val knownIds = fishingActivityRepository.findVesselIds(to.minusDays(SyncFishingActivity.LOOKUP_DAYS))
        vessels.forEach { (vessel, importedLocode) ->
            var status = current
            try {
                val ids =
                    knownIds[vessel.cfr] ?: fishingActivityClient
                        .findVesselIds(VesselIdentity(vessel.mmsi, vessel.callSign, vessel.name, FLAG))
                        .also { fishingActivityRepository.saveVesselIds(vessel.cfr, it) }
                val visits = if (ids.isEmpty()) null else fishingActivityClient.findPortVisits(ids, from, to)
                val check = check(vessel, importedLocode, visits, ports, to)
                fishingActivityRepository.saveHomePortCheck(check)
                status =
                    status.copy(
                        countsByResult =
                            status.countsByResult + (check.result to (status.countsByResult[check.result] ?: 0) + 1),
                    )
                Thread.sleep(FindHomePortsFromPortVisits.DELAY_BETWEEN_VESSELS_MS)
            } catch (e: Exception) {
                logger.warn("Could not check the home port of ${vessel.cfr}: ${e.message}")
                status =
                    status.copy(
                        failed = status.failed + 1,
                        errors = (status.errors + "${vessel.cfr}: ${e.message}").take(ImportPhotos.MAX_ERRORS),
                    )
            }
            current = status.copy(done = status.done + 1)
        }
        current = current.copy(isRunning = false, finishedAt = ZonedDateTime.now())
        logger.info("Home ports checked: $current")
    }

    companion object {
        /**
         * The imported home port of a vessel compared to the port it visited most; [visits] is null for a vessel not
         * found on Global Fishing Watch
         */
        fun check(
            vessel: HomePortCandidate,
            importedLocode: String,
            visits: List<PortVisit>?,
            ports: List<Port>,
            checkedOn: LocalDate,
        ): HomePortCheck {
            val base =
                HomePortCheck(
                    cfr = vessel.cfr,
                    vesselName = vessel.name,
                    importedLocode = importedLocode,
                    result = HomePortCheckResult.NOT_FOUND,
                    gfwLocode = null,
                    gfwPortName = null,
                    visitsTotal = visits?.size ?: 0,
                    visitsAtGfwPort = 0,
                    visitsAtImported = 0,
                    distanceKm = null,
                    checkedOn = checkedOn,
                )
            if (visits == null) {
                return base
            }
            // The visits to each port of the map, a port of Global Fishing Watch at a time
            val locodeByPortId =
                visits
                    .groupBy { it.portId }
                    .mapValues { (portId, portVisits) ->
                        portOnTheMap(portId, portVisits.first(), ports)?.first?.locode
                    }
            val visitsAtImported = visits.count { locodeByPortId[it.portId] == importedLocode }
            val (portId, portVisits) =
                mostVisitedPort(visits)
                    ?: return base.copy(
                        result = HomePortCheckResult.TOO_FEW_VISITS,
                        visitsAtImported = visitsAtImported,
                    )
            val gfwLocode = locodeByPortId[portId]
            // All the visits to the port of the map, from all its anchorages
            val visitsAtGfwPort =
                if (gfwLocode == null) portVisits.size else visits.count { locodeByPortId[it.portId] == gfwLocode }
            val imported = ports.firstOrNull { it.locode == importedLocode }
            val gfwPort = ports.firstOrNull { it.locode == gfwLocode }
            val visit = portVisits.first()
            val distance =
                imported?.let {
                    distanceKm(
                        it.latitude!!,
                        it.longitude!!,
                        gfwPort?.latitude ?: visit.latitude,
                        gfwPort?.longitude ?: visit.longitude,
                    )
                }
            val result =
                when {
                    gfwLocode == importedLocode -> HomePortCheckResult.AGREES
                    gfwLocode == null -> HomePortCheckResult.NOT_ON_THE_MAP
                    distance != null && distance <= HomePortCheckResult.NEARBY_KM -> HomePortCheckResult.NEARBY
                    else -> HomePortCheckResult.DISAGREES
                }

            return base.copy(
                result = result,
                gfwLocode = gfwLocode,
                gfwPortName = gfwPort?.name ?: visit.portName,
                visitsAtGfwPort = visitsAtGfwPort,
                visitsAtImported = visitsAtImported,
                distanceKm = distance?.let { Math.round(it * 10) / 10.0 },
            )
        }
    }
}
