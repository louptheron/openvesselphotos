package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.Report
import org.openvesselphotos.domain.entities.ReportStatus

interface ReportRepository {
    fun findById(id: Int): Report?

    fun findByStatus(status: ReportStatus): List<Report>

    fun findOpenByPhotoId(photoId: Int): List<Report>

    fun save(report: Report): Report
}
