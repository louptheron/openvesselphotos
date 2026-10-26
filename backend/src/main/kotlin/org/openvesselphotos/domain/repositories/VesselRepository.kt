package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselFilter
import org.openvesselphotos.domain.entities.VesselFilterOptions
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.entities.WikidataData
import java.time.LocalDate

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

    /**
     * Inserts the new vessels. For the existing ones, fills in the missing name, registration, port and port code, and
     * replaces the register's data with the given one.
     */
    fun importAll(vessels: List<Vessel>): VesselImportCounts

    /** Replaces the Wikidata data of the vessels, by id. */
    fun updateWikidata(dataByVesselId: Map<Int, WikidataData>)

    fun count(): Int

    /** Vessels created on each day since [day], imports included (days without any are left out). */
    fun countCreatedByDay(day: LocalDate): Map<LocalDate, Int>

    fun countWithApprovedPhotos(filter: VesselFilter = VesselFilter()): Int

    /** The flags (see [org.openvesselphotos.domain.entities.Flags]), ports, types and gears of vessels with photos */
    fun findFilterOptions(): VesselFilterOptions

    /**
     * Other vessels with approved photos, from the same port first, then under the same flag (from the CFR) or the
     * same MMSI country code (MID). Among them, the closest ids: each vessel links to different ones.
     */
    fun findRelatedWithApprovedPhotos(
        vessel: Vessel,
        limit: Int,
    ): List<Vessel>

    /**
     * Vessels without approved photo but described by the EU fleet register (see [Vessel.isDescribed]), oldest
     * first: their pages are released to search engines a few at a time.
     */
    fun findDescribedWithoutApprovedPhotos(limit: Int): List<Vessel>

    /** How many vessels come before this one in [findDescribedWithoutApprovedPhotos]. */
    fun countDescribedWithoutApprovedPhotosBefore(id: Int): Int

    /** Vessels with at least one approved photo, by name (unnamed last). */
    fun findPageWithApprovedPhotos(
        offset: Int,
        limit: Int,
        filter: VesselFilter = VesselFilter(),
    ): List<Vessel>
}
