package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Report
import org.openvesselphotos.domain.entities.ReportReason
import org.openvesselphotos.domain.entities.ReportStatus
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.ReportRepository
import org.openvesselphotos.domain.repositories.VesselRepository

@UseCase
class ReportPhoto(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
    private val reportRepository: ReportRepository,
) {
    fun execute(
        key: VesselKey,
        number: Int,
        reason: ReportReason,
        message: String?,
    ): Report {
        val notFound =
            BackendUsageException(
                BackendUsageErrorCode.NOT_FOUND,
                "No photo $number for ${key.type.name} ${key.value}.",
            )
        val vesselId = vesselRepository.findByKey(key)?.id ?: throw notFound
        val photo = photoRepository.findApprovedByVesselIdAndNumber(vesselId, number) ?: throw notFound

        return reportRepository.save(
            Report(
                photoId = requireNotNull(photo.id),
                reason = reason,
                message = message?.trim()?.take(1000)?.ifEmpty { null },
                status = ReportStatus.OPEN,
            ),
        )
    }
}
