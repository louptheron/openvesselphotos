package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.DataSource
import org.openvesselphotos.domain.entities.DataUpdate
import org.openvesselphotos.domain.entities.HomePortCorrection
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.DataUpdateRepository
import org.openvesselphotos.domain.repositories.HomePortCorrectionRepository
import org.openvesselphotos.domain.repositories.PortRepository
import org.openvesselphotos.domain.repositories.VesselRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZonedDateTime

/** Approving a home port correction makes it the vessel's home port, kept over the imports and the other sources. */
@UseCase
class ReviewHomePort(
    private val vesselRepository: VesselRepository,
    private val portRepository: PortRepository,
    private val homePortCorrectionRepository: HomePortCorrectionRepository,
    private val dataUpdateRepository: DataUpdateRepository,
) {
    @Transactional
    fun execute(
        correctionId: Int,
        approve: Boolean,
    ): HomePortCorrection {
        val correction =
            homePortCorrectionRepository.findById(correctionId)
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.NOT_FOUND,
                    "No home port correction $correctionId.",
                )
        if (correction.status != ReviewStatus.PENDING) {
            throw BackendUsageException(
                BackendUsageErrorCode.CONFLICT,
                "Home port correction $correctionId was already reviewed.",
            )
        }

        if (approve) {
            val port =
                portRepository.findByLocode(correction.portLocode)
                    ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No port ${correction.portLocode}.")
            val vesselName =
                vesselRepository.findByKey(VesselKey.of(KeyType.CFR, correction.cfr))?.name
                    ?: portRepository.findHomePort(correction.cfr)?.vesselName
            portRepository.setManualHomePort(correction.cfr, vesselName, port.locode)
            dataUpdateRepository.add(
                DataUpdate(
                    DataSource.HOME_PORTS_CORRECTIONS,
                    LocalDate.now(),
                    "Port d’attache de ${vesselName ?: correction.cfr} corrigé : ${port.name}",
                ),
            )
        }

        return homePortCorrectionRepository.save(
            correction.copy(
                status = if (approve) ReviewStatus.APPROVED else ReviewStatus.REJECTED,
                reviewedAt = ZonedDateTime.now(),
            ),
        )
    }
}
