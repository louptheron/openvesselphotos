package org.openvesselphotos.infrastructure.database.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

@Embeddable
data class PhotoFileId(
    @Column(name = "photo_id")
    val photoId: Int,
    val size: String,
) : Serializable

@Entity
@Table(name = "photo_files")
class PhotoFileEntity(
    @EmbeddedId
    val id: PhotoFileId,
    val content: ByteArray,
)
