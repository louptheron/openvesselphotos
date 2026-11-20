package org.openvesselphotos.domain.fishing_activity

import org.openvesselphotos.domain.entities.HomePortCandidate
import java.time.LocalDate

/** The vessels of the ports site on Global Fishing Watch, and where they fished */
interface FishingActivityRepository {
    /**
     * The vessels of the ports site (with a home port), of this port only when given, with their identifiers on the
     * register when known
     */
    fun findPortVessels(locode: String?): List<HomePortCandidate>

    /** Global Fishing Watch identifiers of the vessels by CFR, looked up since [since]; empty for vessels not found */
    fun findVesselIds(since: LocalDate): Map<String, List<String>>

    fun saveVesselIds(
        cfr: String,
        vesselIds: List<String>,
    )

    /** Replaces where the vessel fished */
    fun replaceCells(
        cfr: String,
        cells: List<FishingCell>,
    )

    /**
     * Where the vessels of the port fished, summed by cell; only the vessels with these main gears (FAO codes) when
     * given
     */
    fun findPortCells(
        locode: String,
        gearCodes: List<String>?,
    ): List<FishingCell>

    /** Vessels of the port with fishing cells, with these main gears when given */
    fun countTrackedPortVessels(
        locode: String,
        gearCodes: List<String>?,
    ): Int

    /** The vessels with an imported home port, of this port only when given, with that port's LOCODE */
    fun findImportedHomePorts(locode: String?): List<Pair<HomePortCandidate, String>>

    fun saveHomePortCheck(check: HomePortCheck)

    /** The checks of the imported home ports, with the name of the imported port */
    fun findHomePortChecks(): List<HomePortCheck>
}
