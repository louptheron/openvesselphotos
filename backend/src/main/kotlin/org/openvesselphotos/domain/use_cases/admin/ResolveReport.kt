package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.ReportStatus
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.ReportRepository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@UseCase
class ResolveReport(
    private val photoRepository: PhotoRepository,
    private val reportRepository: ReportRepository,
) {
    /** Either dismisses the report, or removes the photo (closing every open report on it). */
    @Transactional
    fun execute(
        reportId: Int,
        removePhoto: Boolean,
    ) {
        val report =
            reportRepository.findById(reportId)
                ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No report $reportId.")
        if (report.status != ReportStatus.OPEN) {
            throw BackendUsageException(BackendUsageErrorCode.CONFLICT, "Report $reportId was already resolved.")
        }
        val now = ZonedDateTime.now()

        if (!removePhoto) {
            reportRepository.save(report.copy(status = ReportStatus.DISMISSED, reviewedAt = now))
            return
        }

        val photo =
            photoRepository.findById(report.photoId)
                ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No photo ${report.photoId}.")
        photoRepository.deleteFiles(report.photoId)
        photoRepository.save(photo.copy(status = PhotoStatus.REMOVED, reviewedAt = now))
        reportRepository.findOpenByPhotoId(report.photoId).forEach {
            reportRepository.save(it.copy(status = ReportStatus.PHOTO_REMOVED, reviewedAt = now))
        }
    }
}
