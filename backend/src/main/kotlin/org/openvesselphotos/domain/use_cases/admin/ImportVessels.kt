package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.KeyType
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
)

data class VesselImportResult(
    val read: Int,
    val created: Int,
    val updated: Int,
    val unchanged: Int,
    val skipped: Int,
    /** The first errors, for the admin to fix the file */
    val errors: List<String>,
)

/**
 * Creates the vessels of a fleet list (e.g. the EU fleet register), without photos. Existing vessels are never
 * overwritten: only their missing name, registration and port are filled in.
 */
@UseCase
class ImportVessels(
    private val vesselRepository: VesselRepository,
) {
    @Transactional
    fun execute(rows: List<VesselImportRow>): VesselImportResult {
        val errors = mutableListOf<String>()
        // Fleet register exports list each vessel's events: the last line of a vessel is its current state
        val vessels = linkedMapOf<String, Vessel>()
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
                        port = row.port.clean(100),
                    )
                vessels[cfr ?: "mmsi:$mmsi"] = vessel
            } catch (e: BackendUsageException) {
                errors += "Line ${row.line}: ${e.message}"
            }
        }

        val counts = vesselRepository.importAll(vessels.values.toList())

        return VesselImportResult(
            read = rows.size,
            created = counts.created,
            updated = counts.updated,
            unchanged = vessels.size - counts.created - counts.updated,
            skipped = errors.size,
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
    }
}
