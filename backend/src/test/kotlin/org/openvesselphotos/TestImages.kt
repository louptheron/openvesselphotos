package org.openvesselphotos

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object TestImages {
    fun create(
        width: Int,
        height: Int,
        format: String = "jpeg",
    ): ByteArray {
        val type = if (format == "png") BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        val image = BufferedImage(width, height, type)
        image.createGraphics().apply {
            color = Color(207, 224, 234)
            fillRect(0, 0, width, height)
            color = Color(11, 34, 51)
            fillRect(width / 4, height / 2, width / 2, height / 4)
            dispose()
        }
        val output = ByteArrayOutputStream()
        ImageIO.write(image, format, output)

        return output.toByteArray()
    }
}
