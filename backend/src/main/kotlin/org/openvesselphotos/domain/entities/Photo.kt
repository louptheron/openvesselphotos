package org.openvesselphotos.domain.entities

import java.time.LocalDate
import java.time.ZonedDateTime

enum class PhotoStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REMOVED,
}

data class Photo(
    val id: Int? = null,
    val vesselId: Int?,
    /** Public number `n`, set on approval. */
    val number: Int? = null,
    val status: PhotoStatus,
    val submittedKey: VesselKey,
    val submittedVesselName: String?,
    val credit: String,
    val licence: Licence,
    val takenOn: LocalDate?,
    val place: String?,
    val contributorEmail: String?,
    val width: Int,
    val height: Int,
    val createdAt: ZonedDateTime? = null,
    val reviewedAt: ZonedDateTime? = null,
    val rejectReason: String? = null,
    /** The API key the photo was sent with, if any. */
    val apiKeyId: Int? = null,
)
