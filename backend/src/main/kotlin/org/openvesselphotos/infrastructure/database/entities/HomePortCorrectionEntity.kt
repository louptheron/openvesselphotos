package org.openvesselphotos.infrastructure.database.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.openvesselphotos.domain.entities.HomePortCorrection
import org.openvesselphotos.domain.entities.ReviewStatus
import java.time.ZonedDateTime

@Entity
@Table(name = "home_port_corrections")
class HomePortCorrectionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    val cfr: String,
    @Column(name = "port_locode")
    val portLocode: String,
    val comment: String?,
    val contributor: String?,
    @Enumerated(EnumType.STRING)
    val status: ReviewStatus,
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: ZonedDateTime? = null,
    @Column(name = "reviewed_at")
    val reviewedAt: ZonedDateTime?,
) {
    fun toHomePortCorrection() =
        HomePortCorrection(
            id = id,
            cfr = cfr,
            portLocode = portLocode,
            comment = comment,
            contributor = contributor,
            status = status,
            createdAt = createdAt,
            reviewedAt = reviewedAt,
        )

    companion object {
        fun fromHomePortCorrection(correction: HomePortCorrection) =
            HomePortCorrectionEntity(
                id = correction.id,
                cfr = correction.cfr,
                portLocode = correction.portLocode,
                comment = correction.comment,
                contributor = correction.contributor,
                status = correction.status,
                createdAt = correction.createdAt,
                reviewedAt = correction.reviewedAt,
            )
    }
}
