package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.HomePortCorrection
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.HomePortCorrectionRepository
import org.openvesselphotos.domain.repositories.PortRepository
import org.openvesselphotos.domain.repositories.VesselRepository

/** A visitor says a vessel is based at another port: the admin reviews it before it changes anything. */
@UseCase
class SuggestHomePort(
    private val vesselRepository: VesselRepository,
    private val portRepository: PortRepository,
    private val homePortCorrectionRepository: HomePortCorrectionRepository,
) {
    fun execute(
        vesselKey: VesselKey,
        locode: String,
        comment: String?,
        contributor: String?,
    ): HomePortCorrection {
        // A vessel of the ports site may not be known by Open Vessel Photos: it is then found by its home port
        val cfr =
            vesselRepository.findByKey(vesselKey)?.cfr
                ?: vesselKey.value.takeIf { vesselKey.type == KeyType.CFR && portRepository.findHomePort(it) != null }
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.NOT_FOUND,
                    "No vessel with a CFR for ${vesselKey.type.name} ${vesselKey.value}.",
                )
        val port =
            portRepository.findByLocode(locode.trim().uppercase())
                ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No port $locode.")
        if (portRepository.findHomePort(cfr)?.portLocode == port.locode) {
            throw BackendUsageException(
                BackendUsageErrorCode.CONFLICT,
                "This vessel is already based at ${port.name}.",
            )
        }
        if (homePortCorrectionRepository.findByCfrAndStatus(cfr, ReviewStatus.PENDING).any {
                it.portLocode == port.locode
            }
        ) {
            throw BackendUsageException(
                BackendUsageErrorCode.CONFLICT,
                "${port.name} was already suggested for this vessel.",
            )
        }

        return homePortCorrectionRepository.save(
            HomePortCorrection(
                cfr = cfr,
                portLocode = port.locode,
                comment = comment?.trim()?.take(MAX_COMMENT)?.ifEmpty { null },
                contributor = contributor?.trim()?.take(MAX_CONTRIBUTOR)?.ifEmpty { null },
                status = ReviewStatus.PENDING,
            ),
        )
    }

    companion object {
        const val MAX_COMMENT = 500
        const val MAX_CONTRIBUTOR = 100
    }
}
