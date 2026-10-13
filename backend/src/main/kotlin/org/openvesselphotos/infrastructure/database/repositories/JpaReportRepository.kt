package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.Report
import org.openvesselphotos.domain.entities.ReportStatus
import org.openvesselphotos.domain.repositories.ReportRepository
import org.openvesselphotos.infrastructure.database.entities.ReportEntity
import org.openvesselphotos.infrastructure.database.repositories.interfaces.DBReportRepository
import org.springframework.stereotype.Repository

@Repository
class JpaReportRepository(
    private val dbReportRepository: DBReportRepository,
) : ReportRepository {
    override fun findById(id: Int): Report? = dbReportRepository.findById(id).orElse(null)?.toReport()

    override fun findByStatus(status: ReportStatus): List<Report> =
        dbReportRepository.findByStatusOrderByCreatedAt(status).map {
            it.toReport()
        }

    override fun findOpenByPhotoId(photoId: Int): List<Report> =
        dbReportRepository.findByPhotoIdAndStatus(photoId, ReportStatus.OPEN).map { it.toReport() }

    override fun save(report: Report): Report =
        dbReportRepository.saveAndFlush(ReportEntity.fromReport(report)).toReport()
}
