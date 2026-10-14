package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.RegisterData
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
    /** Technical data of the EU fleet register, as written in its exports */
    val vesselType: String? = null,
    val yearBuilt: String? = null,
    val length: String? = null,
    val enginePower: String? = null,
    val tonnage: String? = null,
    val mainGear: String? = null,
    val otherGears: List<String> = emptyList(),
    val callSign: String? = null,
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
 * overwritten: only their missing name, registration and port are filled in. Vessels whose last event is a retirement,
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
                        port = row.port.clean(100),
                        register = toRegisterData(row),
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

    private fun toRegisterData(row: VesselImportRow): RegisterData {
        val mainGear = row.mainGear.toCode()

        return RegisterData(
            vesselType = row.vesselType.toCode(),
            yearBuilt =
                row.yearBuilt
                    ?.trim()
                    ?.toIntOrNull()
                    ?.takeIf { it in 1800..2100 },
            lengthMeters = row.length.toPositiveNumber(MAX_LENGTH_METERS),
            enginePowerKw = row.enginePower.toPositiveNumber(MAX_NUMBER),
            tonnageGt = row.tonnage.toPositiveNumber(MAX_NUMBER),
            mainGear = mainGear,
            otherGears =
                row.otherGears
                    .mapNotNull { it.toCode() }
                    .filter { it != mainGear }
                    .distinct()
                    .take(5),
            callSign =
                row.callSign
                    ?.trim()
                    ?.uppercase()
                    ?.takeIf { CALL_SIGN.matches(it) },
        )
    }

    /** A FAO code (e.g. "OTB"), without "NO" (no gear) */
    private fun String?.toCode(): String? = this?.trim()?.uppercase()?.takeIf { FAO_CODE.matches(it) && it != "NO" }

    /** "9.70" -> 9.7; 0 or less means unknown */
    private fun String?.toPositiveNumber(max: Double): Double? =
        this
            ?.trim()
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?.takeIf { it > 0 && it < max }

    private fun String?.clean(maxLength: Int): String? =
        this
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(maxLength)
            ?.ifEmpty { null }

    companion object {
        const val MAX_ERRORS = 20

        // Below the limits of the database columns
        private const val MAX_LENGTH_METERS = 10_000.0
        private const val MAX_NUMBER = 1_000_000.0
        private val FAO_CODE = Regex("[A-Z0-9]{1,5}")
        private val CALL_SIGN = Regex("[A-Z0-9]{2,10}")

        /** Retirement, destruction and export events of the EU fleet register */
        val OUT_OF_FLEET_EVENTS = setOf("RET", "DES", "EXP")
    }
}
