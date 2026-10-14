package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.VesselRepository
import org.springframework.transaction.annotation.Transactional

/** A vessel read from an import file, before validation. */
data class VesselImportRow(
    val line: Int,
    val cfr: String?,
    val mmsi: String?,
    val name: String?,
    val registration: String?,
    val port: String?,
    /** UN/LOCODE of the port, e.g. FRBES */
    val portCode: String? = null,
    /** Event of the EU fleet register (e.g. "MOD", or "RET" for a retired vessel) */
    val event: String? = null,
)

data class VesselImportResult(
    val read: Int,
    val created: Int,
    val updated: Int,
    val unchanged: Int,
    val skipped: Int,
    /** Vessels no longer in the fleet (retired, destroyed or exported), not imported */
    val outOfFleet: Int,
    /** The first errors, for the admin to fix the file */
    val errors: List<String>,
)

/**
 * Creates the vessels of a fleet list (e.g. the EU fleet register), without photos. Existing vessels are never
 * overwritten: only their missing name, registration, port and port code are filled in. Vessels whose last event is a retirement,
 * a destruction or an export are no longer in the fleet: they are not imported.
 */
@UseCase
class ImportVessels(
    private val vesselRepository: VesselRepository,
) {
    @Transactional
    fun execute(rows: List<VesselImportRow>): VesselImportResult {
        val errors = mutableListOf<String>()
        // Fleet register exports list each vessel's events: the last line of a vessel is its current state
        // null: the vessel left the fleet
        val vessels = linkedMapOf<String, Vessel?>()
        rows.forEach { row ->
            try {
                val cfr = row.cfr?.takeIf { it.isNotBlank() }?.let { VesselKey.of(KeyType.CFR, it).value }
                val mmsi = row.mmsi?.takeIf { it.isNotBlank() }?.let { VesselKey.of(KeyType.MMSI, it).value }
                if (cfr == null && mmsi == null) {
                    errors += "Line ${row.line}: no CFR nor MMSI."
                    return@forEach
                }
                val vessel =
                    Vessel(
                        cfr = cfr,
                        mmsi = mmsi,
                        name = row.name.clean(100),
                        registration = row.registration.clean(20),
                        port = row.port.clean(100)?.let { Port.prettyName(it) },
                        portCode =
                            row.portCode
                                ?.trim()
                                ?.uppercase()
                                ?.takeIf { Port.isCode(it) },
                    )
                val hasLeftFleet = row.event?.trim()?.uppercase() in OUT_OF_FLEET_EVENTS
                vessels[cfr ?: "mmsi:$mmsi"] = vessel.takeUnless { hasLeftFleet }
            } catch (e: BackendUsageException) {
                errors += "Line ${row.line}: ${e.message}"
            }
        }

        val inFleet = vessels.values.filterNotNull()
        val counts = vesselRepository.importAll(inFleet)

        return VesselImportResult(
            read = rows.size,
            created = counts.created,
            updated = counts.updated,
            unchanged = inFleet.size - counts.created - counts.updated,
            skipped = errors.size,
            outOfFleet = vessels.size - inFleet.size,
            errors = errors.take(MAX_ERRORS),
        )
    }

    private fun String?.clean(maxLength: Int): String? =
        this
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(maxLength)
            ?.ifEmpty { null }

    companion object {
        const val MAX_ERRORS = 20

        /** Retirement, destruction and export events of the EU fleet register */
        val OUT_OF_FLEET_EVENTS = setOf("RET", "DES", "EXP")
    }
}
