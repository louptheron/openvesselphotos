package org.openvesselphotos.domain.entities

import java.time.ZonedDateTime

enum class ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED,
}

/** A suggestion to add the missing key (CFR or MMSI) to a vessel. */
data class KeyLinkRequest(
    val id: Int? = null,
    val vesselId: Int,
    val key: VesselKey,
    val contributor: String?,
    val status: ReviewStatus,
    val createdAt: ZonedDateTime? = null,
    val reviewedAt: ZonedDateTime? = null,
)
