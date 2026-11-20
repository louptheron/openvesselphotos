package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.fishing_activity.FishingActivityClient
import org.openvesselphotos.domain.fishing_activity.FishingActivityRepository
import org.openvesselphotos.domain.fishing_activity.FishingCell
import java.time.LocalDate

/** The period of the fishing activity map: the last 12 complete months */
data class FishingEffortPeriod(
    val isEnabled: Boolean,
    val from: LocalDate,
    val to: LocalDate,
    /** The gear families drawn: the data of the others is too scarce, their small vessels lacking AIS */
    val gearFamilies: Set<String>,
)

/** Where the vessels of a port fished over the last 12 months */
data class PortFishingEffort(
    /** Vessels of the port found on Global Fishing Watch with fishing */
    val trackedVessels: Int,
    val cells: List<FishingCell>,
)

/**
 * Where the French fishing vessels fish: tiles of their fishing effort over the last 12 complete months, from their
 * positions (Global Fishing Watch), for a family of gears of the ports site when given (e.g. chalut-de-fond).
 */
@UseCase
class GetFishingEffort(
    private val fishingActivityClient: FishingActivityClient,
    private val fishingActivityRepository: FishingActivityRepository,
) {
    fun period(): FishingEffortPeriod {
        val to = LocalDate.now().withDayOfMonth(1)

        return FishingEffortPeriod(fishingActivityClient.isEnabled, to.minusYears(1), to, GEAR_TYPES.keys)
    }

    /** Where the vessels of the port fished, those of the gear family only when given (by their register gear) */
    fun port(
        locode: String,
        gearFamily: String?,
    ): PortFishingEffort {
        val gearCodes =
            gearFamily?.let {
                GEAR_CODES[it] ?: throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "Unknown gear $it.")
            }
        val port = locode.trim().uppercase()

        return PortFishingEffort(
            trackedVessels = fishingActivityRepository.countTrackedPortVessels(port, gearCodes),
            cells = fishingActivityRepository.findPortCells(port, gearCodes),
        )
    }

    fun tile(
        z: Int,
        x: Int,
        y: Int,
        gearFamily: String?,
    ): ByteArray? {
        if (!fishingActivityClient.isEnabled) {
            throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "The fishing activity map is not enabled.")
        }
        val tiles = 1 shl z
        if (z !in 0..MAX_ZOOM || x !in 0 until tiles || y !in 0 until tiles) {
            throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "No tile $z/$x/$y.")
        }
        val gearTypes =
            gearFamily?.let {
                GEAR_TYPES[it] ?: throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "Unknown gear $it.")
            }

        return fishingActivityClient.fishingEffortTile(z, x, y, FLAG, gearTypes.orEmpty())
    }

    companion object {
        /** Beyond, the map enlarges the tiles of this level: the data is not finer */
        const val MAX_ZOOM = 9
        private const val FLAG = "FRA"

        /**
         * The gear families of the ports site drawn on the map (see frontend/src/ports/gears.tsx), in Global Fishing
         * Watch's gear types: the trawlers only. The other French fishing vessels are mostly too small to carry AIS,
         * and their maps would be nearly empty, misleading.
         */
        val GEAR_TYPES =
            mapOf(
                // Global Fishing Watch does not tell bottom trawls from pelagic ones
                "chalut-de-fond" to listOf("trawlers"),
                "chalut-pelagique" to listOf("trawlers"),
            )

        /** The same families in the FAO codes of the EU fleet register, for the vessels of a port */
        val GEAR_CODES =
            mapOf(
                "chalut-de-fond" to listOf("OTB", "OTT", "PTB", "TBB", "TBN", "TBS", "OT", "TB"),
                "chalut-pelagique" to listOf("OTM", "PTM", "TM", "TMS"),
            )
    }
}
