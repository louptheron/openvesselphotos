package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.PortLocationRepository
import org.openvesselphotos.domain.repositories.PortVesselCounts
import org.openvesselphotos.domain.repositories.VesselRepository

/** The ports of registration of the imported vessels, with their coordinates and how many of their vessels have a photo. */
@UseCase
class GetPorts(
    private val vesselRepository: VesselRepository,
    private val portLocationRepository: PortLocationRepository,
) {
    /** Ordered by number of vessels. */
    fun execute(): List<Port> =
        vesselRepository
            .countByPort()
            .map { toPort(it) }
            .sortedWith(compareByDescending<Port> { it.vessels }.thenBy { it.name })

    fun execute(code: String): Port {
        val normalizedCode = code.uppercase()

        return vesselRepository
            .countByPort(normalizedCode.takeIf { Port.isCode(it) } ?: throw notFound(code))
            .firstOrNull()
            ?.let { toPort(it) }
            ?: throw notFound(code)
    }

    private fun toPort(counts: PortVesselCounts) =
        Port(
            code = counts.code,
            name = counts.name?.let { Port.prettyName(it) } ?: counts.code,
            location = portLocationRepository.findByCode(counts.code),
            vessels = counts.vessels,
            photographed = counts.photographed,
        )

    private fun notFound(code: String) = BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No port $code.")
}
