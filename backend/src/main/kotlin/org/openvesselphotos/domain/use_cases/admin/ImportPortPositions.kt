package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.DataSource
import org.openvesselphotos.domain.entities.DataUpdate
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.repositories.DataUpdateRepository
import org.openvesselphotos.domain.repositories.PortRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** A port's position read from an import file, before validation. */
data class PortPositionImportRow(
    val line: Int,
    val locode: String?,
    val latitude: String?,
    val longitude: String?,
)

data class PortPositionImportResult(
    val read: Int,
    val updated: Int,
    /** Ports not known yet: they come with the home ports of the vessels */
    val unknown: Int,
    val skipped: Int,
    val errors: List<String>,
)

/** Sets the positions of the ports on the map, e.g. from the UN/LOCODE code list or corrected by hand. */
@UseCase
class ImportPortPositions(
    private val portRepository: PortRepository,
    private val dataUpdateRepository: DataUpdateRepository,
) {
    @Transactional
    fun execute(rows: List<PortPositionImportRow>): PortPositionImportResult {
        val errors = mutableListOf<String>()
        val ports = linkedMapOf<String, Port>()
        rows.forEach { row ->
            val locode = row.locode?.trim()?.uppercase()
            val latitude = row.latitude.toCoordinate(90.0)
            val longitude = row.longitude.toCoordinate(180.0)
            when {
                locode == null || !ImportHomePorts.LOCODE.matches(locode) ->
                    errors += "Line ${row.line}: invalid LOCODE \"${row.locode.orEmpty()}\"."
                latitude == null || longitude == null ->
                    errors += "Line ${row.line}: invalid position."
                else ->
                    ports[locode] =
                        Port(
                            locode = locode,
                            name = locode,
                            country = locode.take(2),
                            latitude = latitude,
                            longitude = longitude,
                        )
            }
        }
        val updated = if (ports.isEmpty()) 0 else portRepository.savePositions(ports.values.toList())
        if (updated > 0) {
            dataUpdateRepository.add(
                DataUpdate(DataSource.PORT_POSITIONS, LocalDate.now(), "Positions de $updated ports"),
            )
        }

        return PortPositionImportResult(
            read = rows.size,
            updated = updated,
            unknown = ports.size - updated,
            skipped = errors.size,
            errors = errors.take(ImportVessels.MAX_ERRORS),
        )
    }

    /** "47.5", "47,5" */
    private fun String?.toCoordinate(max: Double): Double? =
        this
            ?.trim()
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?.takeIf { it >= -max && it <= max }
}
