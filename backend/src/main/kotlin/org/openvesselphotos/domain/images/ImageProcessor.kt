package org.openvesselphotos.domain.images

import org.openvesselphotos.domain.entities.ImageSize

data class ProcessedImage(
    val width: Int,
    val height: Int,
    val files: Map<ImageSize, ByteArray>,
)

interface ImageProcessor {
    /** Decodes an uploaded JPEG or PNG and re-encodes it as JPEG in every [ImageSize], without metadata. */
    fun process(bytes: ByteArray): ProcessedImage
}
