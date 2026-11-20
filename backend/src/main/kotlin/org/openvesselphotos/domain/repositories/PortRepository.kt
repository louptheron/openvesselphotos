package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.HomePort
import org.openvesselphotos.domain.entities.HomePortCandidate
import org.openvesselphotos.domain.entities.HomePortSearchResult
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.entities.PortWithVesselCount
import org.openvesselphotos.domain.entities.VesselOwner
import java.time.LocalDate

interface PortRepository {
    /**
     * Replaces the imported home ports with the given ones; they take precedence over the ones found on Global Fishing
     * Watch. The ports are created, or renamed: their positions are kept.
     */
    fun replaceHomePorts(
        ports: List<Port>,
        homePorts: List<HomePort>,
        importedOn: LocalDate,
    )

    /** Sets the positions of the known ports, by locode. Returns the number of ports updated. */
    fun savePositions(ports: List<Port>): Int

    /** The ports with at least one vessel based at them, by name. */
    fun findAllWithVessels(): List<PortWithVesselCount>

    fun findByLocode(locode: String): Port?

    /** The vessels based at the port, by name. */
    fun findHomePorts(locode: String): List<HomePort>

    /** Replaces every vessel owner with the given ones. */
    fun replaceOwners(owners: List<VesselOwner>)

    fun findOwners(cfrs: Collection<String>): List<VesselOwner>

    /** Vessels based at a port whose name contains the query, or whose CFR starts with it; by name */
    fun searchVessels(
        query: String,
        limit: Int,
    ): List<HomePortSearchResult>

    /** French vessels of the EU fleet register (by their CFR) without home port */
    fun findHomePortCandidates(): List<HomePortCandidate>

    /** The day the home ports last changed, by an import or from Global Fishing Watch; null without home port */
    fun findLastHomePortsUpdate(): LocalDate?

    /** Adds home ports to vessels without one; the ones with a home port already keep it */
    fun addHomePorts(homePorts: List<HomePort>)
}
