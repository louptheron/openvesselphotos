package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.DataSource
import org.openvesselphotos.domain.entities.DataUpdate
import org.openvesselphotos.domain.entities.HomePort
import org.openvesselphotos.domain.entities.HomePortSource
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.DataUpdateRepository
import org.openvesselphotos.domain.repositories.PortRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** The returns to port of a vessel at a port, read from an import file, before validation. */
data class PortReturnImportRow(
    val line: Int,
    val cfr: String?,
    val portLocode: String?,
    val returns: String?,
)

data class PortReturnsImportResult(
    /** Lines read */
    val read: Int,
    /** Vessels given the port of most of their returns to port */
    val imported: Int,
    /** Ports of these home ports */
    val ports: Int,
    /** Vessels returning as often at two ports or more, left out: they keep their home port */
    val ties: List<String>,
    val skipped: Int,
    /** The first errors, for the admin to fix the file */
    val errors: List<String>,
)

/**
 * Replaces the home ports from returns to port with the port each vessel of the file returned to most (its port
 * d'exploitation, as Ifremer defines it): they are kept over the other sources, except the ones corrected by a visitor.
 */
@UseCase
class ImportHomePortsFromPortReturns(
    private val portRepository: PortRepository,
    private val dataUpdateRepository: DataUpdateRepository,
) {
    @Transactional
    fun execute(rows: List<PortReturnImportRow>): PortReturnsImportResult {
        val errors = mutableListOf<String>()
        val portReturns = mutableListOf<PortReturn>()
        rows.forEach { row ->
            try {
                portReturns += validate(row)
            } catch (e: BackendUsageException) {
                errors += "Line ${row.line}: ${e.message}"
            }
        }

        val (homePorts, ties) = topPorts(portReturns)
        val ports =
            homePorts
                .map { it.portLocode }
                .distinct()
                .map { Port(locode = it, name = it, country = it.take(2)) }
        if (homePorts.isNotEmpty()) {
            portRepository.replaceHomePortsFromPortReturns(ports, homePorts, LocalDate.now())
            dataUpdateRepository.add(
                DataUpdate(
                    DataSource.HOME_PORTS_PORT_RETURNS,
                    LocalDate.now(),
                    "Ports d’attache de ${homePorts.size} navires, dans ${ports.size} ports, d’après leurs retours au port",
                ),
            )
        }

        return PortReturnsImportResult(
            read = rows.size,
            imported = homePorts.size,
            ports = ports.size,
            ties = ties,
            skipped = errors.size,
            errors = errors.take(ImportVessels.MAX_ERRORS),
        )
    }

    private fun validate(row: PortReturnImportRow): PortReturn {
        val cfr =
            row.cfr?.let { VesselKey.of(KeyType.CFR, it).value }
                ?: throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "no CFR.")
        val locode =
            row.portLocode
                ?.trim()
                ?.uppercase()
                ?.takeIf { ImportHomePorts.LOCODE.matches(it) }
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.INVALID_INPUT,
                    "invalid port LOCODE \"${row.portLocode.orEmpty()}\".",
                )
        val count =
            row.returns
                ?.trim()
                ?.toIntOrNull()
                ?.takeIf { it in 1..MAX_RETURNS }
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.INVALID_INPUT,
                    "invalid number of returns to port \"${row.returns.orEmpty()}\".",
                )

        return PortReturn(cfr, locode, count)
    }

    /** The returns to port of a vessel at a port */
    data class PortReturn(
        val cfr: String,
        val portLocode: String,
        val count: Int,
    )

    companion object {
        private const val MAX_RETURNS = 100_000

        /**
         * The port each vessel returned to most, with its returns there among all its returns; and the vessels returning
         * as often to two ports or more, by CFR. The returns of a vessel to a port listed twice are added up.
         */
        fun topPorts(portReturns: List<PortReturn>): Pair<List<HomePort>, List<String>> {
            val ties = mutableListOf<String>()
            val homePorts =
                portReturns.groupBy { it.cfr }.mapNotNull { (cfr, vesselReturns) ->
                    val byPort =
                        vesselReturns.groupBy { it.portLocode }.mapValues { (_, it) ->
                            it.sumOf { l -> l.count }
                        }
                    val top = byPort.values.max()
                    val topPorts = byPort.filterValues { it == top }.keys
                    if (topPorts.size > 1) {
                        ties += cfr
                        return@mapNotNull null
                    }

                    HomePort(
                        cfr = cfr,
                        vesselName = null,
                        portLocode = topPorts.single(),
                        observationsTotal = byPort.values.sum(),
                        observationsAtPort = top,
                        meanDistanceKm = null,
                        source = HomePortSource.RETURNS,
                    )
                }

            return homePorts to ties
        }
    }
}
