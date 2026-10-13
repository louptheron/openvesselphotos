package org.openvesselphotos.domain.entities

import java.time.ZonedDateTime

enum class ReportReason {
    WRONG_VESSEL,
    COPYRIGHT,
    PRIVACY,
    OTHER,
}

enum class ReportStatus {
    OPEN,
    DISMISSED,
    PHOTO_REMOVED,
}

data class Report(
    val id: Int? = null,
    val photoId: Int,
    val reason: ReportReason,
    val message: String?,
    val status: ReportStatus,
    val createdAt: ZonedDateTime? = null,
    val reviewedAt: ZonedDateTime? = null,
)
