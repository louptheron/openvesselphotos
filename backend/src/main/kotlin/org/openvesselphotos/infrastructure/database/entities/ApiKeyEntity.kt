package org.openvesselphotos.infrastructure.database.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.openvesselphotos.domain.entities.ApiKey
import org.openvesselphotos.domain.entities.ApiKeyStatus
import java.time.ZonedDateTime

@Entity
@Table(name = "api_keys")
class ApiKeyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    val name: String,
    val email: String,
    val organisation: String?,
    @Column(name = "intended_use")
    val intendedUse: String,
    @Enumerated(EnumType.STRING)
    val status: ApiKeyStatus,
    @Column(name = "key_hash", columnDefinition = "bpchar(64)")
    val keyHash: String?,
    @Column(name = "key_prefix")
    val keyPrefix: String?,
    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: ZonedDateTime? = null,
    @Column(name = "reviewed_at")
    val reviewedAt: ZonedDateTime?,
    @Column(name = "last_used_at")
    val lastUsedAt: ZonedDateTime?,
) {
    fun toApiKey() =
        ApiKey(
            id = id,
            name = name,
            email = email,
            organisation = organisation,
            intendedUse = intendedUse,
            status = status,
            keyHash = keyHash,
            keyPrefix = keyPrefix,
            createdAt = createdAt,
            reviewedAt = reviewedAt,
            lastUsedAt = lastUsedAt,
        )

    companion object {
        fun fromApiKey(apiKey: ApiKey) =
            ApiKeyEntity(
                id = apiKey.id,
                name = apiKey.name,
                email = apiKey.email,
                organisation = apiKey.organisation,
                intendedUse = apiKey.intendedUse,
                status = apiKey.status,
                keyHash = apiKey.keyHash,
                keyPrefix = apiKey.keyPrefix,
                createdAt = apiKey.createdAt,
                reviewedAt = apiKey.reviewedAt,
                lastUsedAt = apiKey.lastUsedAt,
            )
    }
}
