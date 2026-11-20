package org.openvesselphotos.infrastructure.web

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** What a link to a page of the ports site shows when shared (og:image): its name and key figures, sourced */
data class ShareCard(
    /** e.g. "Port de pêche · FRLRT" */
    val eyebrow: String,
    /** e.g. "Lorient" */
    val title: String,
    /** At most 3: value (e.g. "16") and label (e.g. "navires de pêche") */
    val figures: List<Pair<String, String>>,
    /** e.g. "Sources : FranceAgriMer 2025, registre de la flotte de l’UE" */
    val source: String,
)

/**
 * Draws the [ShareCard]s as 1200×630 PNG images, the size social networks and messaging apps show large. In IBM Plex
 * Sans, bundled (SIL Open Font License, see fonts/IBM-Plex-LICENSE.txt): the server may have no font of its own.
 */
@Component
class ShareImageRenderer {
    private val regular = loadFont("fonts/IBMPlexSans-Regular.ttf")
    private val semiBold = loadFont("fonts/IBMPlexSans-SemiBold.ttf")

    fun render(
        card: ShareCard,
        host: String,
    ): ByteArray {
        val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            draw(graphics, card, host)
        } finally {
            graphics.dispose()
        }

        return ByteArrayOutputStream().use {
            ImageIO.write(image, "png", it)
            it.toByteArray()
        }
    }

    private fun draw(
        graphics: Graphics2D,
        card: ShareCard,
        host: String,
    ) {
        graphics.color = GROUND
        graphics.fillRect(0, 0, WIDTH, HEIGHT)
        graphics.color = INK
        graphics.fillRect(0, 0, WIDTH, 10)

        // The mark and the name of the site
        drawMark(graphics, MARGIN.toDouble(), 64.0, 56.0)
        graphics.font = semiBold.deriveFont(30f)
        graphics.color = INK
        graphics.drawString(Site.PORTS.siteName, MARGIN + 76, 92)
        graphics.font = regular.deriveFont(21f)
        graphics.color = MUTED
        graphics.drawString(PortsSeo.TAGLINE.replaceFirstChar { it.uppercase() }, MARGIN + 76, 118)

        // What the page is about
        graphics.font = semiBold.deriveFont(22f)
        graphics.color = ACCENT_INK
        graphics.drawString(card.eyebrow.uppercase(), MARGIN, 212)
        var titleSize = 76f
        graphics.font = semiBold.deriveFont(titleSize)
        while (graphics.fontMetrics.stringWidth(card.title) > WIDTH - 2 * MARGIN && titleSize > 40f) {
            titleSize -= 4f
            graphics.font = semiBold.deriveFont(titleSize)
        }
        graphics.color = INK
        graphics.drawString(ellipsize(graphics, card.title, WIDTH - 2 * MARGIN), MARGIN, 300)

        // Its key figures
        val columnWidth = (WIDTH - 2 * MARGIN) / FIGURES
        card.figures.take(FIGURES).forEachIndexed { index, (value, label) ->
            val x = MARGIN + index * columnWidth
            graphics.color = DIVIDER
            graphics.fillRect(x, 352, columnWidth - 32, 2)
            // Long values, e.g. the name of an auction, in a smaller size rather than cut
            var valueSize = 54f
            graphics.font = semiBold.deriveFont(valueSize)
            while (graphics.fontMetrics.stringWidth(value) > columnWidth - 32 && valueSize > 30f) {
                valueSize -= 2f
                graphics.font = semiBold.deriveFont(valueSize)
            }
            graphics.color = INK
            graphics.drawString(ellipsize(graphics, value, columnWidth - 32), x, 428)
            graphics.font = regular.deriveFont(22f)
            graphics.color = MUTED
            graphics.drawString(ellipsize(graphics, label, columnWidth - 32), x, 464)
        }

        // Its sources, and where it is
        graphics.color = DIVIDER
        graphics.fillRect(MARGIN, 534, WIDTH - 2 * MARGIN, 2)
        graphics.font = regular.deriveFont(20f)
        graphics.color = MUTED
        val hostWidth = graphics.fontMetrics.stringWidth(host)
        graphics.drawString(ellipsize(graphics, card.source, WIDTH - 2 * MARGIN - hostWidth - 32), MARGIN, 576)
        graphics.font = semiBold.deriveFont(20f)
        graphics.color = INK
        graphics.drawString(host, WIDTH - MARGIN - hostWidth, 576)
    }

    /** The mark of frontend/src/ports/SiteMark.tsx, on its 32×32 grid: a fish whose body holds bars of a chart */
    private fun drawMark(
        graphics: Graphics2D,
        x: Double,
        y: Double,
        size: Double,
    ) {
        val saved = graphics.transform
        graphics.translate(x, y)
        graphics.scale(size / 32, size / 32)
        graphics.color = INK
        graphics.fill(RoundRectangle2D.Double(0.0, 0.0, 32.0, 32.0, 12.0, 12.0))
        graphics.color = Color.WHITE
        graphics.fill(
            Path2D.Double().apply {
                moveTo(9.0, 16.0)
                lineTo(4.0, 10.5)
                lineTo(4.0, 21.5)
                closePath()
            },
        )
        graphics.fill(
            Path2D.Double().apply {
                moveTo(8.0, 16.0)
                curveTo(11.0, 10.5, 15.5, 8.5, 19.0, 8.5)
                curveTo(23.5, 8.5, 26.5, 12.0, 28.0, 16.0)
                curveTo(26.5, 20.0, 23.5, 23.5, 19.0, 23.5)
                curveTo(15.5, 23.5, 11.0, 21.5, 8.0, 16.0)
                closePath()
            },
        )
        graphics.color = INK
        graphics.stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        graphics.draw(Line2D.Double(12.5, 14.2, 20.5, 14.2))
        graphics.draw(Line2D.Double(12.5, 17.8, 17.5, 17.8))
        graphics.color = ACCENT
        graphics.fill(Ellipse2D.Double(22.8, 12.6, 2.8, 2.8))
        graphics.transform = saved
    }

    private fun ellipsize(
        graphics: Graphics2D,
        text: String,
        maxWidth: Int,
    ): String {
        val metrics = graphics.fontMetrics
        if (metrics.stringWidth(text) <= maxWidth) return text
        var shortened = text
        while (shortened.isNotEmpty() && metrics.stringWidth("$shortened…") > maxWidth) {
            shortened = shortened.dropLast(1)
        }

        return "${shortened.trimEnd()}…"
    }

    companion object {
        const val WIDTH = 1200
        const val HEIGHT = 630
        private const val MARGIN = 72
        private const val FIGURES = 3

        // As in frontend/src/theme.ts
        private val GROUND = Color(0xF5F3EE)
        private val INK = Color(0x0B2233)
        private val INK_SOFT = Color(0xB9C8D3)
        private val MUTED = Color(0x55697A)
        private val DIVIDER = Color(0xDAD5CA)
        private val ACCENT = Color(0xE0662A)
        private val ACCENT_INK = Color(0xB5501C)

        private fun loadFont(path: String): Font =
            ClassPathResource(path).inputStream.use { Font.createFont(Font.TRUETYPE_FONT, it) }
    }
}
