package org.openvesselphotos.infrastructure.images

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.images.ImageProcessor
import org.openvesselphotos.domain.images.ProcessedImage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.locks.ReentrantLock
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Re-encodes every upload as JPEG with the JDK only. Re-encoding drops all metadata (EXIF, GPS position).
 */
@Component
class JavaImageProcessor : ImageProcessor {
    private val logger = LoggerFactory.getLogger(JavaImageProcessor::class.java)

    /** One image at a time: decoding and resizing are the most memory-hungry operations of the app. */
    private val processingLock = ReentrantLock()

    override fun process(bytes: ByteArray): ProcessedImage = processingLock.withLock { processOne(bytes) }

    private fun processOne(bytes: ByteArray): ProcessedImage {
        val decoded = decode(bytes)
        val image = toRgb(applyExifOrientation(decoded, readExifOrientation(bytes)))

        val full = scaleToFit(image, ImageSize.FULL.pixels)
        val files =
            ImageSize.entries.associateWith { size ->
                val scaled =
                    when (size) {
                        ImageSize.FULL -> full
                        else -> scaleToWidth(full, size.pixels)
                    }
                encodeJpeg(scaled)
            }

        return ProcessedImage(width = full.width, height = full.height, files = files)
    }

    /**
     * Decodes the image at a reduced resolution when it is larger than needed (source subsampling), so that a
     * 24-megapixel photo never sits in memory at full size: this keeps the app within a small container.
     */
    private fun decode(bytes: ByteArray): BufferedImage {
        val invalidImage =
            BackendUsageException(BackendUsageErrorCode.INVALID_IMAGE, "The file is not a JPEG or PNG image.")
        val input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: throw invalidImage

        return input.use {
            val reader =
                ImageIO
                    .getImageReaders(
                        input,
                    ).asSequence()
                    .firstOrNull { it.formatName.lowercase() in SUPPORTED_FORMATS }
                    ?: throw invalidImage
            try {
                reader.input = input
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                if (width < MIN_WIDTH || height < MIN_HEIGHT) {
                    throw BackendUsageException(
                        BackendUsageErrorCode.INVALID_IMAGE,
                        "The image is too small ($width×$height), it must be at least ${MIN_WIDTH}×$MIN_HEIGHT pixels.",
                    )
                }
                if (width.toLong() * height > MAX_PIXELS) {
                    throw BackendUsageException(
                        BackendUsageErrorCode.INVALID_IMAGE,
                        "The image is too large ($width×$height).",
                    )
                }

                // Keeps at least the full size's resolution; the exact size is reached by resizing
                val subsampling = (max(width, height) / ImageSize.FULL.pixels).coerceAtLeast(1)
                val params = reader.defaultReadParam.apply { setSourceSubsampling(subsampling, subsampling, 0, 0) }

                reader.read(0, params)
            } catch (e: BackendUsageException) {
                throw e
            } catch (e: Exception) {
                logger.info("Could not decode an uploaded image: ${e.message}")
                throw invalidImage
            } finally {
                reader.dispose()
            }
        }
    }

    private fun readExifOrientation(bytes: ByteArray): Int =
        try {
            ImageMetadataReader
                .readMetadata(ByteArrayInputStream(bytes))
                .getFirstDirectoryOfType(ExifIFD0Directory::class.java)
                ?.takeIf { it.containsTag(ExifIFD0Directory.TAG_ORIENTATION) }
                ?.getInt(ExifIFD0Directory.TAG_ORIENTATION) ?: 1
        } catch (e: Exception) {
            1
        }

    private fun applyExifOrientation(
        image: BufferedImage,
        orientation: Int,
    ): BufferedImage {
        if (orientation !in 2..8) {
            return image
        }
        val width = image.width.toDouble()
        val height = image.height.toDouble()
        val swapsAxes = orientation >= 5
        val transform =
            AffineTransform().apply {
                when (orientation) {
                    2 -> {
                        translate(width, 0.0)
                        scale(-1.0, 1.0)
                    }
                    3 -> {
                        translate(width, height)
                        rotate(Math.PI)
                    }
                    4 -> {
                        translate(0.0, height)
                        scale(1.0, -1.0)
                    }
                    5 -> {
                        rotate(Math.PI / 2)
                        scale(1.0, -1.0)
                    }
                    6 -> {
                        translate(height, 0.0)
                        rotate(Math.PI / 2)
                    }
                    7 -> {
                        scale(-1.0, 1.0)
                        translate(-height, 0.0)
                        translate(0.0, width)
                        rotate(3 * Math.PI / 2)
                    }
                    8 -> {
                        translate(0.0, width)
                        rotate(3 * Math.PI / 2)
                    }
                }
            }
        val result =
            BufferedImage(
                if (swapsAxes) image.height else image.width,
                if (swapsAxes) image.width else image.height,
                BufferedImage.TYPE_INT_RGB,
            )
        result.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            drawImage(image, transform, null)
            dispose()
        }

        return result
    }

    /** Flattens transparency on white and drops color models JPEG cannot store. */
    private fun toRgb(image: BufferedImage): BufferedImage {
        if (image.type == BufferedImage.TYPE_INT_RGB) {
            return image
        }
        val result = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        result.createGraphics().apply {
            color = Color.WHITE
            fillRect(0, 0, image.width, image.height)
            drawImage(image, 0, 0, null)
            dispose()
        }

        return result
    }

    private fun scaleToFit(
        image: BufferedImage,
        maxLongEdge: Int,
    ): BufferedImage {
        val longEdge = max(image.width, image.height)
        if (longEdge <= maxLongEdge) {
            return image
        }
        val ratio = maxLongEdge.toDouble() / longEdge

        return resize(image, (image.width * ratio).roundToInt(), (image.height * ratio).roundToInt())
    }

    private fun scaleToWidth(
        image: BufferedImage,
        width: Int,
    ): BufferedImage {
        if (image.width <= width) {
            return image
        }

        return resize(image, width, (image.height * width.toDouble() / image.width).roundToInt())
    }

    /** Halves the image step by step before the last resize, which keeps downscaled photos sharp. */
    private fun resize(
        image: BufferedImage,
        width: Int,
        height: Int,
    ): BufferedImage {
        var current = image
        while (current.width / 2 >= width && current.height / 2 >= height) {
            current = draw(current, current.width / 2, current.height / 2)
        }

        return if (current.width == width && current.height == height) current else draw(current, width, height)
    }

    private fun draw(
        image: BufferedImage,
        width: Int,
        height: Int,
    ): BufferedImage {
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        result.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            drawImage(image, 0, 0, width, height, null)
            dispose()
        }

        return result
    }

    private fun encodeJpeg(image: BufferedImage): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val output = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(output).use { stream ->
            writer.output = stream
            val params =
                writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = JPEG_QUALITY
                }
            writer.write(null, IIOImage(image, null, null), params)
            writer.dispose()
        }

        return output.toByteArray()
    }

    companion object {
        const val MIN_WIDTH = 320
        const val MIN_HEIGHT = 200
        const val JPEG_QUALITY = 0.82f

        /** 100 megapixels: above, it is not a photo but an attempt to exhaust the memory. */
        const val MAX_PIXELS = 100_000_000L
        private val SUPPORTED_FORMATS = setOf("jpeg", "jpg", "png")
    }
}
