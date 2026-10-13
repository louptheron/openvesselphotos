package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselKey

interface VesselRepository {
    fun findById(id: Int): Vessel?

    fun findByKey(key: VesselKey): Vessel?

    fun findByIds(ids: Collection<Int>): List<Vessel>

    fun search(
        query: String,
        limit: Int,
    ): List<Vessel>

    /** Vessels ordered by their latest approved photo. */
    fun findRecentlyPhotographed(limit: Int): List<Vessel>

    fun findAllWithApprovedPhotos(): List<Vessel>

    fun save(vessel: Vessel): Vessel
}
