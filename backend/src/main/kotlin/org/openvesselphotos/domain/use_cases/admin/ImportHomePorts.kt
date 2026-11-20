package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.DataSource
import org.openvesselphotos.domain.entities.DataUpdate
import org.openvesselphotos.domain.entities.HomePort
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.DataUpdateRepository
import org.openvesselphotos.domain.repositories.PortRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** A vessel's home port read from an import file, before validation. */
data class HomePortImportRow(
    val line: Int,
    val cfr: String?,
    val vesselName: String?,
    val portLocode: String?,
    val portName: String?,
    val portCountry: String?,
    val observationsTotal: String?,
    val observationsAtPort: String?,
    val meanDistanceKm: String?,
)

data class HomePortImportResult(
    val read: Int,
    val imported: Int,
    val ports: Int,
    /** Vessels without a home port (never observed at a port) */
    val withoutPort: Int,
    val skipped: Int,
    /** The first errors, for the admin to fix the file */
    val errors: List<String>,
)

/** Replaces the home ports of the vessels with the ones of the file. Vessels without a port are left out. */
@UseCase
class ImportHomePorts(
    private val portRepository: PortRepository,
    private val dataUpdateRepository: DataUpdateRepository,
) {
    @Transactional
    fun execute(rows: List<HomePortImportRow>): HomePortImportResult {
        val errors = mutableListOf<String>()
        val ports = linkedMapOf<String, Port>()
        val homePorts = linkedMapOf<String, HomePort>()
        var withoutPort = 0
        rows.forEach { row ->
            try {
                val cfr =
                    row.cfr?.takeIf { it.isNotBlank() }?.let { VesselKey.of(KeyType.CFR, it).value }
                        ?: throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "no CFR.")
                val locode =
                    row.portLocode
                        ?.trim()
                        ?.uppercase()
                        ?.takeIf { it.isNotEmpty() }
                if (locode == null) {
                    withoutPort++
                    return@forEach
                }
                if (!LOCODE.matches(locode)) {
                    errors += "Line ${row.line}: invalid port LOCODE \"$locode\"."
                    return@forEach
                }
                val observationsTotal = row.observationsTotal.toCount()
                val observationsAtPort = row.observationsAtPort.toCount()
                if (observationsTotal == null || observationsAtPort == null || observationsAtPort > observationsTotal) {
                    errors += "Line ${row.line}: invalid observation counts."
                    return@forEach
                }

                ports.getOrPut(locode) {
                    Port(
                        locode = locode,
                        name = row.portName.clean(100) ?: locode,
                        country =
                            row.portCountry?.trim()?.uppercase()?.takeIf {
                                COUNTRY.matches(
                                    it,
                                )
                            } ?: locode.take(2),
                    )
                }
                homePorts[cfr] =
                    HomePort(
                        cfr = cfr,
                        vesselName = row.vesselName.clean(100),
                        portLocode = locode,
                        observationsTotal = observationsTotal,
                        observationsAtPort = observationsAtPort,
                        meanDistanceKm =
                            row.meanDistanceKm
                                ?.trim()
                                ?.toDoubleOrNull()
                                ?.takeIf { it >= 0 && it < MAX_DISTANCE_KM },
                    )
            } catch (e: BackendUsageException) {
                errors += "Line ${row.line}: ${e.message}"
            }
        }

        if (homePorts.isNotEmpty()) {
            portRepository.replaceHomePorts(ports.values.toList(), homePorts.values.toList(), LocalDate.now())
            dataUpdateRepository.add(
                DataUpdate(
                    DataSource.HOME_PORTS,
                    LocalDate.now(),
                    "Ports d’attache de ${homePorts.size} navires, dans ${ports.size} ports",
                ),
            )
        }

        return HomePortImportResult(
            read = rows.size,
            imported = homePorts.size,
            ports = ports.size,
            withoutPort = withoutPort,
            skipped = errors.size,
            errors = errors.take(ImportVessels.MAX_ERRORS),
        )
    }

    private fun String?.toCount(): Int? =
        this
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it in 0..MAX_OBSERVATIONS }

    companion object {
        /** e.g. FRQUY, FRSQ2: a country code and three letters or digits, and a terminal's number (FRLRH-0015) */
        val LOCODE = Regex("[A-Z]{2}[A-Z0-9]{3}(-[0-9]{1,4})?")
        val COUNTRY = Regex("[A-Z]{2}")
        private const val MAX_OBSERVATIONS = 1_000_000
        private const val MAX_DISTANCE_KM = 100_000.0
    }
}

internal fun String?.clean(maxLength: Int): String? =
    this
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.take(maxLength)
        ?.ifEmpty { null }
