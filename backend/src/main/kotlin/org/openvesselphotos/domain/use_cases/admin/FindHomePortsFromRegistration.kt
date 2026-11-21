package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.DataSource
import org.openvesselphotos.domain.entities.DataUpdate
import org.openvesselphotos.domain.entities.HomePort
import org.openvesselphotos.domain.entities.HomePortSource
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.repositories.DataUpdateRepository
import org.openvesselphotos.domain.repositories.PortRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

data class HomePortsFromRegistrationResult(
    /** French vessels of the EU fleet register without home port, or with one from their place of registration */
    val candidates: Int,
    /** Vessels given (or given again) the port of their place of registration */
    val placed: Int,
    /** Without place of registration in the register */
    val withoutPlace: Int,
    /** Places of registration and the port they were matched to, e.g. "CAEN → Port-en-Bessin (FRPEB), 12 navires" */
    val matched: List<String>,
    /** Places of registration matched to no port of the map, e.g. "SAINT PIERRE (14)" */
    val unmatched: List<String>,
)

/**
 * Gives the French vessels of the EU fleet register without home port the port of their place of registration (their
 * quartier maritime, e.g. "LE GUILVINEC"): a port of the map of the same name, else the port most of the vessels of
 * this quartier with a known home port are based at. The coarsest source: a quartier covers several ports, so these
 * home ports never replace an observed one, and are replaced by the ones found later (import, Global Fishing Watch).
 */
@UseCase
class FindHomePortsFromRegistration(
    private val portRepository: PortRepository,
    private val dataUpdateRepository: DataUpdateRepository,
) {
    @Transactional
    fun execute(): HomePortsFromRegistrationResult {
        val candidates = portRepository.findHomePortCandidates()
        val ports =
            portRepository.findAllWithVessels().map { it.port }.filter {
                it.latitude != null &&
                    it.longitude != null
            }
        val homePortCounts = portRepository.countHomePortsByPlaceOfRegistration()
        val byPlace = candidates.filter { !it.placeOfRegistration.isNullOrBlank() }.groupBy { it.placeOfRegistration!! }
        val portByPlace = byPlace.keys.associateWith { portOf(it, ports, homePortCounts[it].orEmpty()) }
        val homePorts =
            byPlace.flatMap { (place, vessels) ->
                val port = portByPlace[place] ?: return@flatMap emptyList()
                vessels.map {
                    HomePort(
                        cfr = it.cfr,
                        vesselName = it.name,
                        portLocode = port.locode,
                        observationsTotal = 0,
                        observationsAtPort = 0,
                        meanDistanceKm = null,
                        source = HomePortSource.REGISTER,
                    )
                }
            }
        val withoutHomePort = candidates.filter { it.source == null }.map { it.cfr }.toSet()
        val newOnes = homePorts.count { it.cfr in withoutHomePort }
        if (homePorts.isNotEmpty()) {
            portRepository.addHomePorts(homePorts)
        }
        if (newOnes > 0) {
            dataUpdateRepository.add(
                DataUpdate(
                    DataSource.HOME_PORTS_REGISTER,
                    LocalDate.now(),
                    "Ports d’attache de $newOnes navires de plus, d’après leur quartier d’immatriculation",
                ),
            )
        }

        return HomePortsFromRegistrationResult(
            candidates = candidates.size,
            placed = homePorts.size,
            withoutPlace = candidates.size - byPlace.values.sumOf { it.size },
            matched =
                portByPlace.entries
                    .filter { it.value != null }
                    .sortedByDescending { byPlace[it.key]!!.size }
                    .map { (place, port) -> "$place → ${port!!.name} (${port.locode}), ${byPlace[place]!!.size}" },
            unmatched =
                portByPlace.entries
                    .filter { it.value == null }
                    .sortedByDescending { byPlace[it.key]!!.size }
                    .map { (place, _) -> "$place (${byPlace[place]!!.size})" },
        )
    }

    companion object {
        /** Fewer vessels of a quartier with a known home port tell nothing of where its vessels are based */
        const val MIN_LOCATED_VESSELS = 3

        /** Quartiers named after a town or island, by normalized name: the LOCODE of their fishing port */
        val ALIASES =
            ImportAuctionSales.ALIASES +
                mapOf(
                    "iledoleron" to "FRLC5",
                    "iledenoirmoutier" to "FRHID",
                )

        /**
         * The port of the map of a place of registration: by its name (or [ALIASES]), else the port most of its vessels
         * with a known home port are based at, when there are [MIN_LOCATED_VESSELS] of them at least
         */
        fun portOf(
            placeOfRegistration: String,
            ports: List<Port>,
            homePortCounts: Map<String, Int>,
        ): Port? {
            ALIASES[ImportAuctionSales.normalize(placeOfRegistration)]?.let { locode ->
                ports.find { it.locode == locode }?.let { return it }
            }
            val name = ImportAuctionSales.normalize(placeOfRegistration)
            val sameName = ports.filter { ImportAuctionSales.normalize(it.name) == name }
            // e.g. Saint-Pierre, in La Réunion and in Saint-Pierre-et-Miquelon: the one its vessels are based at
            if (sameName.size >
                1
            ) {
                return sameName
                    .filter {
                        homePortCounts[it.locode] != null
                    }.maxByOrNull { homePortCounts[it.locode]!! }
            }
            (sameName.singleOrNull() ?: ImportAuctionSales.matchPort(placeOfRegistration, ports))?.let { return it }
            if (homePortCounts.values.sum() < MIN_LOCATED_VESSELS) return null
            val (locode, _) = homePortCounts.maxBy { it.value }

            return ports.find { it.locode == locode }
        }
    }
}
