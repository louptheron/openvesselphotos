package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.use_cases.public_api.GetVesselsPage
import org.openvesselphotos.domain.use_cases.public_api.GetVesselsWithoutPhotosToIndex
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
class CrawlersController(
    private val publicUrl: PublicUrl,
    private val getVesselsPage: GetVesselsPage,
    private val getVesselsWithoutPhotosToIndex: GetVesselsWithoutPhotosToIndex,
    private val vesselsWithoutPhotosRelease: VesselsWithoutPhotosRelease,
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

    /**
     * Sitemap index: the static pages, then every vessel page with photos by files of [VESSELS_PER_SITEMAP], then the
     * vessels without photo released so far.
     */
    @GetMapping("/sitemap.xml")
    fun sitemapIndex(): ResponseEntity<String> {
        val baseUrl = publicUrl.get()
        val vesselSitemaps = getVesselsPage.pageCount(VESSELS_PER_SITEMAP)
        val sitemaps =
            listOf("$baseUrl/sitemap-pages.xml") + (1..vesselSitemaps).map { "$baseUrl/sitemap-vessels-$it.xml" } +
                "$baseUrl/sitemap-vessels-without-photos.xml"

        return xml(
            """<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""" +
                sitemaps.joinToString("") { "<sitemap><loc>$it</loc></sitemap>" } +
                "</sitemapindex>",
        )
    }

    /** The static pages, and each page of the vessel directory, in every language. */
    @GetMapping("/sitemap-pages.xml")
    fun pagesSitemap(): ResponseEntity<String> {
        val baseUrl = publicUrl.get()
        val directoryPages = getVesselsPage.pageCount(GetVesselsPage.DIRECTORY_PAGE_SIZE)

        return xml(
            URLSET_OPEN +
                (
                    listOf(
                        "/",
                        "/vessels",
                        "/docs",
                        "/contribute",
                        "/api-key",
                    ) + (2..directoryPages).map { PagesController.directoryPath(it) }
                ).joinToString("") { urls(baseUrl, it) } +
                "</urlset>",
        )
    }

    /** Vessel pages in every language, with their photos (Google image sitemap extension). */
    @GetMapping("/sitemap-vessels-{page:[0-9]+}.xml")
    fun vesselsSitemap(
        @PathVariable page: Int,
    ): ResponseEntity<String> {
        val baseUrl = publicUrl.get()
        val vessels = getVesselsPage.execute(page.coerceAtLeast(1), VESSELS_PER_SITEMAP)
        if (vessels.isEmpty()) {
            return ResponseEntity.notFound().build()
        }
        val urls =
            vessels.joinToString("") { (vessel, photos) ->
                val lastModified = photos.mapNotNull { it.reviewedAt }.maxOrNull()?.toLocalDate()
                val images =
                    photos.joinToString("") {
                        "<image:image><image:loc>${escapeHtml(
                            PagesController.photoUrl(baseUrl, vessel, it, ImageSize.FULL),
                        )}</image:loc></image:image>"
                    }

                urls(
                    baseUrl,
                    PagesController.vesselPath(vessel),
                    lastModified?.let { "<lastmod>$it</lastmod>" }.orEmpty(),
                    images,
                )
            }

        return xml(URLSET_OPEN + urls + "</urlset>")
    }

    /**
     * Pages of vessels without photo described by the EU fleet register, a few more each day (see
     * [VesselsWithoutPhotosRelease]).
     */
    @GetMapping("/sitemap-vessels-without-photos.xml")
    fun vesselsWithoutPhotosSitemap(): ResponseEntity<String> {
        val baseUrl = publicUrl.get()
        val urls =
            getVesselsWithoutPhotosToIndex
                .execute(vesselsWithoutPhotosRelease.limit())
                .joinToString("") { urls(baseUrl, PagesController.vesselPath(it)) }

        return xml(URLSET_OPEN + urls + "</urlset>")
    }

    /**
     * A page's URL in each language, each linked to the others (hreflang). The photos are listed under the English
     * URL only: they are the same images.
     */
    private fun urls(
        baseUrl: String,
        path: String,
        lastModified: String = "",
        images: String = "",
    ): String {
        val alternates =
            Language.entries.joinToString("") {
                """<xhtml:link rel="alternate" hreflang="${it.code}" href="${escapeHtml(baseUrl + it.path(path))}"/>"""
            } + """<xhtml:link rel="alternate" hreflang="x-default" href="${escapeHtml(baseUrl + path)}"/>"""

        return Language.entries.joinToString("") {
            "<url><loc>${escapeHtml(baseUrl + it.path(path))}</loc>$lastModified$alternates" +
                "${if (it == Language.EN) images else ""}</url>"
        }
    }

    private fun xml(body: String): ResponseEntity<String> =
        ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_XML)
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body("""<?xml version="1.0" encoding="UTF-8"?>$body""")

    companion object {
        const val VESSELS_PER_SITEMAP = 10_000
        private const val URLSET_OPEN =
            """<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" """ +
                """xmlns:image="http://www.google.com/schemas/sitemap-image/1.1" """ +
                """xmlns:xhtml="http://www.w3.org/1999/xhtml">"""
    }
}
