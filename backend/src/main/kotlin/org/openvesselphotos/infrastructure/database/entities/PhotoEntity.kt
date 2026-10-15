package org.openvesselphotos.infrastructure.database.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Licence
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.VesselKey
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(name = "photos")
class PhotoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "vessel_id")
    val vesselId: Int?,
    val number: Int?,
    @Enumerated(EnumType.STRING)
    val status: PhotoStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "submitted_key_type")
    val submittedKeyType: KeyType,
    @Column(name = "submitted_key")
    val submittedKey: String,
    /** Of the other type than [submittedKeyType] */
    @Column(name = "submitted_other_key")
    val submittedOtherKey: String?,
    @Column(name = "submitted_vessel_name")
    val submittedVesselName: String?,
    val credit: String,
    @Enumerated(EnumType.STRING)
    val licence: Licence,
    @Column(name = "taken_on")
    val takenOn: LocalDate?,
    val place: String?,
    @Column(name = "contributor_email")
    val contributorEmail: String?,
    val width: Int,
    val height: Int,
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: ZonedDateTime? = null,
    @Column(name = "reviewed_at")
    val reviewedAt: ZonedDateTime?,
    @Column(name = "reject_reason")
    val rejectReason: String?,
    @Column(name = "api_key_id")
    val apiKeyId: Int?,
    @Column(name = "source_url")
    val sourceUrl: String?,
    val note: String?,
) {
    fun toPhoto() =
        Photo(
            id = id,
            vesselId = vesselId,
            number = number,
            status = status,
            submittedKey = VesselKey(submittedKeyType, submittedKey),
            submittedOtherKey = submittedOtherKey?.let { VesselKey(submittedKeyType.other, it) },
            submittedVesselName = submittedVesselName,
            credit = credit,
            licence = licence,
            takenOn = takenOn,
            place = place,
            contributorEmail = contributorEmail,
            width = width,
            height = height,
            createdAt = createdAt,
            reviewedAt = reviewedAt,
            rejectReason = rejectReason,
            apiKeyId = apiKeyId,
            sourceUrl = sourceUrl,
            note = note,
        )

    companion object {
        fun fromPhoto(photo: Photo) =
            PhotoEntity(
                id = photo.id,
                vesselId = photo.vesselId,
                number = photo.number,
                status = photo.status,
                submittedKeyType = photo.submittedKey.type,
                submittedKey = photo.submittedKey.value,
                submittedOtherKey = photo.submittedOtherKey?.value,
                submittedVesselName = photo.submittedVesselName,
                credit = photo.credit,
                licence = photo.licence,
                takenOn = photo.takenOn,
                place = photo.place,
                contributorEmail = photo.contributorEmail,
                width = photo.width,
                height = photo.height,
                createdAt = photo.createdAt,
                reviewedAt = photo.reviewedAt,
                rejectReason = photo.rejectReason,
                apiKeyId = photo.apiKeyId,
                sourceUrl = photo.sourceUrl,
                note = photo.note,
            )
    }
}
