package org.openvesselphotos.domain.entities

import java.time.ZonedDateTime

/** A home port suggested for a vessel, by its CFR: once approved, it replaces the one found by any other source. */
data class HomePortCorrection(
    val id: Int? = null,
    val cfr: String,
    val portLocode: String,
    val comment: String?,
    val contributor: String?,
    val status: ReviewStatus,
    val createdAt: ZonedDateTime? = null,
    val reviewedAt: ZonedDateTime? = null,
)
