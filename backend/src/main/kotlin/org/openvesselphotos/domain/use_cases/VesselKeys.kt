package org.openvesselphotos.domain.use_cases

import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.VesselRepository

/**
 * The vessel known under any of these keys (a CFR, an MMSI or both), or null. Throws when the keys belong to two
 * different vessels, or when the vessel already has another value for one of them.
 */
internal fun findVesselByKeys(
    vesselRepository: VesselRepository,
    keys: List<VesselKey>,
): Vessel? {
    val vessels = keys.mapNotNull { vesselRepository.findByKey(it) }.distinctBy { it.id }
    if (vessels.size > 1) {
        throw BackendUsageException(
            BackendUsageErrorCode.CONFLICT,
            "${keys.joinToString(" and ") { "${it.type.name} ${it.value}" }} belong to two different vessels.",
        )
    }
    val vessel = vessels.firstOrNull() ?: return null
    keys.forEach { key ->
        val current = vessel.keyValue(key.type)
        if (current != null && current != key.value) {
            throw BackendUsageException(
                BackendUsageErrorCode.CONFLICT,
                "This vessel's ${key.type.name} is $current, not ${key.value}.",
            )
        }
    }

    return vessel
}

/** The vessel known under these keys, completed with its missing keys and name, or a new vessel. */
internal fun findOrCreateVessel(
    vesselRepository: VesselRepository,
    keys: List<VesselKey>,
    name: String?,
): Vessel {
    val existing = findVesselByKeys(vesselRepository, keys)
    val vessel =
        keys.fold(
            existing ?: Vessel(cfr = null, mmsi = null, name = name),
        ) { vessel, key -> vessel.withKey(key) }
    val completed = if (vessel.name == null && name != null) vessel.copy(name = name) else vessel

    return if (existing == null || completed != existing) vesselRepository.save(completed) else existing
}
