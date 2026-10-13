package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.use_cases.public_api.ExportDataset
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
class CrawlersController(
    private val publicUrl: PublicUrl,
    private val exportDataset: ExportDataset,
) {
    @GetMapping("/robots.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun robots(): String =
        """
        |User-agent: *
        |Allow: /
        |Disallow: /admin
        |Disallow: /v1/admin/
        |Disallow: /v1/search
        |
        |Sitemap: ${publicUrl.get()}/sitemap.xml
        |
        """.trimMargin()

    /** Every vessel page with its photos (Google image sitemap extension). */
    @GetMapping("/sitemap.xml")
    fun sitemap(): ResponseEntity<String> {
        val baseUrl = publicUrl.get()
        val vessels = exportDataset.execute()
        val vesselUrls =
            vessels.joinToString("") { (vessel, photos) ->
                val lastModified = photos.mapNotNull { it.reviewedAt }.maxOrNull()?.toLocalDate()
                val images =
                    photos.joinToString("") {
                        "<image:image><image:loc>${escapeHtml(
                            PagesController.photoUrl(baseUrl, vessel, it, ImageSize.FULL),
                        )}</image:loc></image:image>"
                    }

                "<url><loc>$baseUrl${PagesController.vesselPath(
                    vessel,
                )}</loc>${lastModified?.let { "<lastmod>$it</lastmod>" } ?: ""}$images</url>"
            }
        val staticUrls = listOf("/", "/docs", "/contribute").joinToString("") { "<url><loc>$baseUrl$it</loc></url>" }

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_XML)
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(
                """<?xml version="1.0" encoding="UTF-8"?>""" +
                    """<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" """ +
                    """xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">""" +
                    staticUrls + vesselUrls + "</urlset>",
            )
    }
}
