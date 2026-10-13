package org.openvesselphotos.infrastructure.database.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.openvesselphotos.domain.entities.Report
import org.openvesselphotos.domain.entities.ReportReason
import org.openvesselphotos.domain.entities.ReportStatus
import java.time.ZonedDateTime

@Entity
@Table(name = "reports")
class ReportEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "photo_id")
    val photoId: Int,
    @Enumerated(EnumType.STRING)
    val reason: ReportReason,
    val message: String?,
    @Enumerated(EnumType.STRING)
    val status: ReportStatus,
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: ZonedDateTime? = null,
    @Column(name = "reviewed_at")
    val reviewedAt: ZonedDateTime?,
) {
    fun toReport() =
        Report(
            id = id,
            photoId = photoId,
            reason = reason,
            message = message,
            status = status,
            createdAt = createdAt,
            reviewedAt = reviewedAt,
        )

    companion object {
        fun fromReport(report: Report) =
            ReportEntity(
                id = report.id,
                photoId = report.photoId,
                reason = report.reason,
                message = report.message,
                status = report.status,
                createdAt = report.createdAt,
                reviewedAt = report.reviewedAt,
            )
    }
}
