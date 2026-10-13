package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselKey

data class VesselImportCounts(
    val created: Int,
    val updated: Int,
)

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

    /** Inserts the new vessels; fills in the missing name, registration and port of the existing ones. */
    fun importAll(vessels: List<Vessel>): VesselImportCounts

    fun count(): Int

    /** Vessels ordered by id, with or without photos. */
    fun findPage(
        offset: Int,
        limit: Int,
    ): List<Vessel>
}
