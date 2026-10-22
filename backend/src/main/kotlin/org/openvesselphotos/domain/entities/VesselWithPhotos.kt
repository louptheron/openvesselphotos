package org.openvesselphotos.domain.entities

import java.time.ZonedDateTime

/** A vessel and its approved photos, ordered by number (the first one is the main photo). */
data class VesselWithPhotos(
    val vessel: Vessel,
    val photos: List<Photo>,
)

enum class HistoryEventType {
    VESSEL_ADDED,
    PHOTO_PUBLISHED,
    KEY_LINKED,
}

/** An event of a vessel's history. [description] is in English; the other fields let clients translate it. */
data class HistoryEvent(
    val date: ZonedDateTime,
    val type: HistoryEventType,
    val description: String,
    val by: String?,
    val photoNumber: Int? = null,
    val licence: String? = null,
    /** The key added, e.g. "MMSI 227000101" */
    val key: String? = null,
    /** The vessel's other key it was linked to, e.g. "CFR FRA000123456" */
    val linkedTo: String? = null,
)

data class VesselDetails(
    val vessel: Vessel,
    val photos: List<Photo>,
    val history: List<HistoryEvent>,
    /** Other vessels with photos, from the same port or under the same flag */
    val related: List<VesselWithPhotos> = emptyList(),
)
