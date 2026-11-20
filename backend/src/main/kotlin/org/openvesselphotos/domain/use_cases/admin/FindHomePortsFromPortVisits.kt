package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.HomePort
import org.openvesselphotos.domain.entities.HomePortCandidate
import org.openvesselphotos.domain.entities.HomePortSource
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.fishing_activity.FishingActivityClient
import org.openvesselphotos.domain.fishing_activity.PortVisit
import org.openvesselphotos.domain.fishing_activity.VesselIdentity
import org.openvesselphotos.domain.repositories.PortRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class HomePortsFromPortVisitsStatus(
    val isRunning: Boolean,
    /** French vessels of the EU fleet register without home port */
    val total: Int,
    val done: Int,
    /** Vessels found on Global Fishing Watch */
    val found: Int,
    /** Vessels given a home port */
    val placed: Int,
    /** Found, but with too few port visits in the year */
    val withoutVisits: Int,
    /** Found, but their most visited port is not on the map */
    val farFromPorts: Int,
    /** The most visited ports that are not on the map, e.g. "SAINT-PIERRE (12 vessels)" */
    val unknownPorts: List<String>,
    val failed: Int,
    val errors: List<String>,
    val startedAt: ZonedDateTime?,
    val finishedAt: ZonedDateTime?,
)

/**
 * Gives a home port to the French vessels of the EU fleet register that have none: the port each visited most in the
 * last year, from its positions (AIS) on Global Fishing Watch, when that port is on the map (within [MAX_DISTANCE_KM]).
 * Runs in the background, a vessel at a time; the admin follows the progress with [status]. Small vessels without AIS
 * are not found. Imported home ports take precedence over these.
 */
@UseCase
class FindHomePortsFromPortVisits(
    private val portRepository: PortRepository,
    private val fishingActivityClient: FishingActivityClient,
) {
    private val logger = LoggerFactory.getLogger(FindHomePortsFromPortVisits::class.java)

    @Volatile
    private var current =
        HomePortsFromPortVisitsStatus(false, 0, 0, 0, 0, 0, 0, emptyList(), 0, emptyList(), null, null)

    fun status(): HomePortsFromPortVisitsStatus = current

    @Synchronized
    fun start(): HomePortsFromPortVisitsStatus {
        if (!fishingActivityClient.isEnabled) {
            throw BackendUsageException(
                BackendUsageErrorCode.INVALID_INPUT,
                "No Global Fishing Watch token: set OPENVESSELPHOTOS_GFW_TOKEN.",
            )
        }
        if (current.isRunning) {
            throw BackendUsageException(BackendUsageErrorCode.CONFLICT, "A search of home ports is already running.")
        }
        val candidates = portRepository.findHomePortCandidates()
        val ports =
            portRepository.findAllWithVessels().map { it.port }.filter {
                it.latitude != null &&
                    it.longitude != null
            }
        current =
            HomePortsFromPortVisitsStatus(
                isRunning = candidates.isNotEmpty(),
                total = candidates.size,
                done = 0,
                found = 0,
                placed = 0,
                withoutVisits = 0,
                farFromPorts = 0,
                unknownPorts = emptyList(),
                failed = 0,
                errors = emptyList(),
                startedAt = ZonedDateTime.now(),
                finishedAt = if (candidates.isEmpty()) ZonedDateTime.now() else null,
            )
        if (candidates.isNotEmpty()) {
            Thread.ofVirtual().name("gfw-home-ports").start { run(candidates, ports) }
        }

        return current
    }

    private fun run(
        candidates: List<HomePortCandidate>,
        ports: List<Port>,
    ) {
        logger.info("Looking for the home ports of ${candidates.size} vessels on Global Fishing Watch.")
        val to = LocalDate.now()
        val from = to.minusYears(1)
        val unknownPorts = mutableMapOf<String, Int>()
        val homePorts = mutableListOf<HomePort>()
        candidates.forEach { candidate ->
            var status = current
            try {
                val ids =
                    fishingActivityClient.findVesselIds(
                        VesselIdentity(candidate.mmsi, candidate.callSign, candidate.name, FLAG),
                    )
                if (ids.isNotEmpty()) {
                    status = status.copy(found = status.found + 1)
                    val visits = fishingActivityClient.findPortVisits(ids, from, to)
                    when (val result = homePortOf(candidate, visits, ports)) {
                        is Result.Placed -> {
                            homePorts += result.homePort
                            status = status.copy(placed = status.placed + 1)
                        }
                        Result.TooFewVisits -> status = status.copy(withoutVisits = status.withoutVisits + 1)
                        is Result.NotOnTheMap -> {
                            unknownPorts.merge(result.portName, 1, Int::plus)
                            status = status.copy(farFromPorts = status.farFromPorts + 1)
                        }
                    }
                }
                if (homePorts.size >= SAVE_EVERY) {
                    portRepository.addHomePorts(homePorts.toList())
                    homePorts.clear()
                }
                Thread.sleep(DELAY_BETWEEN_VESSELS_MS)
            } catch (e: Exception) {
                logger.warn("Could not find the home port of ${candidate.cfr}: ${e.message}")
                status =
                    status.copy(
                        failed = status.failed + 1,
                        errors = (status.errors + "${candidate.cfr}: ${e.message}").take(ImportPhotos.MAX_ERRORS),
                    )
            }
            current =
                status.copy(
                    done = status.done + 1,
                    unknownPorts =
                        unknownPorts.entries
                            .sortedByDescending { it.value }
                            .take(MAX_UNKNOWN_PORTS)
                            .map { (name, count) -> "$name ($count)" },
                )
        }
        if (homePorts.isNotEmpty()) {
            portRepository.addHomePorts(homePorts)
        }
        current = current.copy(isRunning = false, finishedAt = ZonedDateTime.now())
        logger.info("Search of home ports finished: $current")
    }

    sealed interface Result {
        data class Placed(
            val homePort: HomePort,
        ) : Result

        data object TooFewVisits : Result

        data class NotOnTheMap(
            val portName: String,
        ) : Result
    }

    companion object {
        const val FLAG = "FRA"
        private const val SAVE_EVERY = 50
        private const val MAX_UNKNOWN_PORTS = 20

        /** About 3 requests per vessel: well below the 50,000 requests a day of the API */
        const val DELAY_BETWEEN_VESSELS_MS = 300L

        /** A port visited less in a year is not a home port */
        const val MIN_VISITS = 2

        /** Between the port of the visits and a port of the map */
        const val MAX_DISTANCE_KM = 10.0

        /**
         * The port the vessel visited most, when it was there [MIN_VISITS] times at least and it is on the map: in its
         * own country when it went there enough, as offshore vessels also land abroad (e.g. in Ireland).
         */
        fun homePortOf(
            candidate: HomePortCandidate,
            visits: List<PortVisit>,
            ports: List<Port>,
        ): Result {
            val (portId, portVisits) = mostVisitedPort(visits) ?: return Result.TooFewVisits
            val visit = portVisits.first()
            val (port, distance) = portOnTheMap(portId, visit, ports) ?: return Result.NotOnTheMap(visit.portName)

            return Result.Placed(
                HomePort(
                    cfr = candidate.cfr,
                    vesselName = candidate.name,
                    portLocode = port.locode,
                    observationsTotal = visits.size,
                    observationsAtPort = portVisits.size,
                    meanDistanceKm = Math.round(distance * 100) / 100.0,
                    source = HomePortSource.GFW,
                ),
            )
        }

        /**
         * The port (Global Fishing Watch id) the vessel visited most, with its visits, when it was there [MIN_VISITS]
         * times at least: in its own country when it went there enough, as offshore vessels also land abroad (e.g. in
         * Ireland).
         */
        fun mostVisitedPort(visits: List<PortVisit>): Pair<String, List<PortVisit>>? {
            val visitsByPort = visits.groupBy { it.portId }

            return (
                visitsByPort.entries
                    .filter { (_, portVisits) -> portVisits.first().portFlag == FLAG && portVisits.size >= MIN_VISITS }
                    .maxByOrNull { it.value.size }
                    ?: visitsByPort.maxByOrNull { it.value.size }
            )?.toPair()?.takeIf { it.second.size >= MIN_VISITS }
        }

        /**
         * The port of the map of a port of Global Fishing Watch, with the distance between them: a port of the same name
         * first, e.g. Lorient for fra-lorient, whose anchorage may be nearer another port; else the nearest
         */
        fun portOnTheMap(
            portId: String,
            visit: PortVisit,
            ports: List<Port>,
        ): Pair<Port, Double>? {
            val withDistances =
                ports.map { it to distanceKm(visit.latitude, visit.longitude, it.latitude!!, it.longitude!!) }
            val name =
                ImportAuctionSales.normalize(
                    visit.portName.takeUnless { it == portId } ?: portId.substringAfter("-"),
                )

            return withDistances
                .filter { (port, distance) -> distance <= MAX_NAMED_DISTANCE_KM && isSameName(port.name, name) }
                .minByOrNull { it.second }
                ?: withDistances.minByOrNull { it.second }?.takeIf { it.second <= MAX_DISTANCE_KM }
        }

        /** Between the port of the visits and a port of the map of the same name */
        const val MAX_NAMED_DISTANCE_KM = 25.0

        private fun isSameName(
            portName: String,
            normalizedName: String,
        ): Boolean {
            val name = ImportAuctionSales.normalize(portName)

            return name == normalizedName ||
                (
                    minOf(name.length, normalizedName.length) >= MIN_NAME_LENGTH &&
                        (name.contains(normalizedName) || normalizedName.contains(name))
                )
        }

        private const val MIN_NAME_LENGTH = 5

        /** Great-circle distance */
        fun distanceKm(
            latitude1: Double,
            longitude1: Double,
            latitude2: Double,
            longitude2: Double,
        ): Double {
            val toRadians = Math.PI / 180
            val a =
                sin((latitude2 - latitude1) * toRadians / 2).pow(2) +
                    cos(latitude1 * toRadians) * cos(latitude2 * toRadians) *
                    sin((longitude2 - longitude1) * toRadians / 2).pow(2)

            return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
        }

        private const val EARTH_RADIUS_KM = 6371.0
    }
}
