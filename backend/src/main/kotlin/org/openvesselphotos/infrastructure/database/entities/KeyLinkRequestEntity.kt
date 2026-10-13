package org.openvesselphotos.infrastructure.database.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.openvesselphotos.domain.entities.KeyLinkRequest
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.domain.entities.VesselKey
import java.time.ZonedDateTime

@Entity
@Table(name = "key_link_requests")
class KeyLinkRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "vessel_id")
    val vesselId: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "key_type")
    val keyType: KeyType,
    @Column(name = "key_value")
    val keyValue: String,
    val contributor: String?,
    @Enumerated(EnumType.STRING)
    val status: ReviewStatus,
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: ZonedDateTime? = null,
    @Column(name = "reviewed_at")
    val reviewedAt: ZonedDateTime?,
) {
    fun toKeyLinkRequest() =
        KeyLinkRequest(
            id = id,
            vesselId = vesselId,
            key = VesselKey(keyType, keyValue),
            contributor = contributor,
            status = status,
            createdAt = createdAt,
            reviewedAt = reviewedAt,
        )

    companion object {
        fun fromKeyLinkRequest(request: KeyLinkRequest) =
            KeyLinkRequestEntity(
                id = request.id,
                vesselId = request.vesselId,
                keyType = request.key.type,
                keyValue = request.key.value,
                contributor = request.contributor,
                status = request.status,
                createdAt = request.createdAt,
                reviewedAt = request.reviewedAt,
            )
    }
}
