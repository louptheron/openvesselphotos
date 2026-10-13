package org.openvesselphotos.infrastructure.images

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openvesselphotos.TestImages
import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class JavaImageProcessorUTests {
    private val processor = JavaImageProcessor()

    private fun dimensions(bytes: ByteArray) = ImageIO.read(ByteArrayInputStream(bytes)).let { it.width to it.height }

    @Test
    fun `process Should produce every size`() {
        val result = processor.process(TestImages.create(3000, 2000))

        assertThat(result.width to result.height).isEqualTo(1600 to 1067)
        assertThat(dimensions(result.files.getValue(ImageSize.FULL))).isEqualTo(1600 to 1067)
        assertThat(dimensions(result.files.getValue(ImageSize.MEDIUM))).isEqualTo(800 to 534)
        assertThat(dimensions(result.files.getValue(ImageSize.SMALL))).isEqualTo(320 to 213)
    }

    @Test
    fun `process Should handle a 24-megapixel photo`() {
        val result = processor.process(TestImages.create(6000, 4000))

        assertThat(dimensions(result.files.getValue(ImageSize.FULL))).isEqualTo(1600 to 1067)
    }

    @Test
    fun `process Should convert a transparent PNG to JPEG`() {
        val result = processor.process(TestImages.create(640, 480, "png"))

        assertThat(dimensions(result.files.getValue(ImageSize.MEDIUM))).isEqualTo(640 to 480)
    }

    @Test
    fun `process Should reject a file that is not an image, or too small`() {
        assertThatThrownBy { processor.process("not an image".toByteArray()) }
            .hasFieldOrPropertyWithValue("code", BackendUsageErrorCode.INVALID_IMAGE)
        assertThatThrownBy { processor.process(TestImages.create(200, 100)) }
            .hasFieldOrPropertyWithValue("code", BackendUsageErrorCode.INVALID_IMAGE)
    }
}
