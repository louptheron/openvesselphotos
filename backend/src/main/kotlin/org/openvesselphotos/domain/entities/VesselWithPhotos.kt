package org.openvesselphotos.domain.entities

import java.time.ZonedDateTime

/** A vessel and its approved photos, ordered by number (the first one is the main photo). */
data class VesselWithPhotos(
    val vessel: Vessel,
    val photos: List<Photo>,
)

data class HistoryEvent(
    val date: ZonedDateTime,
    val description: String,
    val by: String?,
)

data class VesselDetails(
    val vessel: Vessel,
    val photos: List<Photo>,
    val history: List<HistoryEvent>,
)
