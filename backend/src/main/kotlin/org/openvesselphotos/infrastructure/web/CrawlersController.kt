package org.openvesselphotos.infrastructure.web

import jakarta.servlet.http.HttpServletRequest
import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.use_cases.public_api.GetRecentVessels
import org.openvesselphotos.domain.use_cases.public_api.GetVesselsPage
import org.openvesselphotos.domain.use_cases.public_api.GetVesselsWithoutPhotosToIndex
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@RestController
class CrawlersController(
    private val publicUrl: PublicUrl,
    private val getVesselsPage: GetVesselsPage,
    private val getRecentVessels: GetRecentVessels,
    private val getVesselsWithoutPhotosToIndex: GetVesselsWithoutPhotosToIndex,
    private val vesselsWithoutPhotosRelease: VesselsWithoutPhotosRelease,
    private val indexNowNotifier: IndexNowNotifier,
) {
    /**
     * Every crawler is welcome, AI search and assistant ones too: they cite the vessel pages in their answers. These are
     * named in a group of their own, so that a host's default rules for AI crawlers (e.g. Cloudflare's managed
     * robots.txt) are not read as this site's wish. A crawler follows only the group naming it, which repeats the
     * disallowed paths.
     */
    @GetMapping("/robots.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun robots(): String {
        val rules =
            """
            |Allow: /
            |Disallow: /admin
            |Disallow: /v1/admin/
            |Disallow: /v1/search
            """.trimMargin()

        return "User-agent: *\n$rules\n\n" +
            AI_CRAWLERS.joinToString("\n") { "User-agent: $it" } + "\n$rules\n\n" +
            "Sitemap: ${publicUrl.get()}/sitemap.xml\n"
    }

    /**
     * What the site is and where its content is, for AI assistants and the people using them (llmstxt.org). In
     * English, with the French pages under /fr.
     */
    @GetMapping("/llms.txt", produces = ["text/markdown;charset=UTF-8"])
    fun llms(): ResponseEntity<String> {
        val baseUrl = publicUrl.get()
        val texts = PageTexts.of(Language.EN)
        val body =
            """
            |# ${HtmlPageRenderer.SITE_NAME}
            |
            |> ${texts.homeDescription}
            |
            |Each vessel has one page, by its CFR (EU fleet register number, e.g. FRA000752221) or its MMSI (e.g.
            |227000000): $baseUrl/vessels/cfr/{CFR} or $baseUrl/vessels/mmsi/{MMSI}. A vessel page gives its name, its
            |marking and port of registration, its technical data from the EU fleet register, and its photos, each with
            |its photographer's credit, its licence (CC0, CC BY, CC BY-SA or Licence Ouverte) and where and when it was
            |taken. When reusing a photo, credit its photographer and name its licence. Every page is also in French,
            |under $baseUrl/fr.
            |
            |## Pages
            |
            |- [Vessel directory]($baseUrl/vessels): every vessel with photos, by flag, quartier d'immatriculation,
            |  type and gear
            |- [API documentation]($baseUrl/docs): ${texts.docsDescription}
            |- [Licences]($baseUrl/docs#licences): the licences of the photos and how to credit them
            |- [Contribute a photo]($baseUrl/contribute): ${texts.contributeDescription}
            |
            |## Data
            |
            |- A vessel and its photos, in JSON: `$baseUrl/v1/cfr/{CFR}` or `$baseUrl/v1/mmsi/{MMSI}`
            |- A vessel's first photo, to embed: `$baseUrl/v1/cfr/{CFR}.jpg` or `$baseUrl/v1/mmsi/{MMSI}.jpg`
            |- [Export of every vessel with photos]($baseUrl/v1/export.json)
            |- [Latest photos]($baseUrl$FEED_PATH): Atom feed
            |- [Sitemap]($baseUrl/sitemap.xml): every vessel page
            |
            """.trimMargin()

        return ResponseEntity
            .ok()
            .contentType(MediaType("text", "markdown", Charsets.UTF_8))
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(body)
    }

    /** Proves to IndexNow search engines that the URLs come from this site (see [IndexNowNotifier]) */
    @GetMapping(IndexNowNotifier.KEY_PATH, produces = [MediaType.TEXT_PLAIN_VALUE])
    fun indexNowKey(): ResponseEntity<String> =
        indexNowNotifier.key?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    /**
     * Sitemap index: the static pages, then every vessel page with photos by files of [VESSELS_PER_SITEMAP], then the
     * vessels without photo released so far. Each with the day it last changed, so that search engines fetch again
     * only the changed ones.
     */
    @GetMapping("/sitemap.xml")
    fun sitemapIndex(): ResponseEntity<String> {
        val baseUrl = publicUrl.get()
        val vesselSitemapDays = getVesselsPage.lastModifiedByPage(VESSELS_PER_SITEMAP)
        val sitemaps =
            // The directory, the quartiers and the home page list the vessels with photos
            listOf("$baseUrl/sitemap-pages.xml" to vesselSitemapDays.lastOrNull()) +
                vesselSitemapDays.mapIndexed { index, day -> "$baseUrl/sitemap-vessels-${index + 1}.xml" to day } +
                // A few more every day
                ("$baseUrl/sitemap-vessels-without-photos.xml" to LocalDate.now())

        return xml(
            """<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""" +
                sitemaps.joinToString("") { (url, day) ->
                    "<sitemap><loc>$url</loc>${day?.let { "<lastmod>$it</lastmod>" }.orEmpty()}</sitemap>"
                } +
                "</sitemapindex>",
        )
    }

    /**
     * The static pages, each page of the vessel directory, and the directory of each quartier d'immatriculation with
     * vessels with photos, in every language.
     */
    @GetMapping("/sitemap-pages.xml")
    fun pagesSitemap(): ResponseEntity<String> {
        val baseUrl = publicUrl.get()
        val directoryPages = getVesselsPage.pageCount(GetVesselsPage.DIRECTORY_PAGE_SIZE)
        val quartiers = getVesselsPage.filterOptions().ports.map { PagesController.quartierPath(it.value) }

        return xml(
            URLSET_OPEN +
                (
                    listOf(
                        "/",
                        "/vessels",
                        "/docs",
                        "/contribute",
                        "/api-key",
                    ) + (2..directoryPages).map { PagesController.directoryPath(it) } + quartiers
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

    /**
     * Atom feed of the latest photos, one entry each: for people following the new photos, and for search engines,
     * which find the changed pages faster from a feed than from the sitemap.
     */
    @GetMapping(FEED_PATH, "/fr$FEED_PATH")
    fun feed(request: HttpServletRequest): ResponseEntity<String> {
        val language = Language.of(request)
        val texts = PageTexts.of(language)
        val baseUrl = publicUrl.get()
        val photos =
            getRecentVessels
                .execute(FEED_SIZE)
                .flatMap { (vessel, photos) -> photos.map { vessel to it } }
                .filter { (_, photo) -> photo.reviewedAt != null }
                .sortedByDescending { (_, photo) -> photo.reviewedAt }
                .take(FEED_SIZE)
        val entries =
            photos.joinToString("") { (vessel, photo) ->
                val pageUrl = baseUrl + language.path(PagesController.vesselPath(vessel))
                val caption = texts.photoCaption(PagesController.vesselName(vessel), photo.displayedCredit)
                val imageUrl = PagesController.photoUrl(baseUrl, vessel, photo, ImageSize.MEDIUM)
                val content =
                    """<p><a href="${escapeHtml(pageUrl)}"><img src="${escapeHtml(imageUrl)}" """ +
                        """alt="${escapeHtml(caption)}" /></a></p>""" +
                        """<p>© ${escapeHtml(photo.displayedCredit)} · ${escapeHtml(photo.licence.label)}</p>"""

                "<entry><id>${escapeHtml("$pageUrl#photo-${photo.number}")}</id>" +
                    "<title>${escapeHtml(caption)}</title>" +
                    """<link rel="alternate" type="text/html" href="${escapeHtml(pageUrl)}"/>""" +
                    "<updated>${atomDate(photo.reviewedAt)}</updated>" +
                    "<author><name>${escapeHtml(photo.displayedCredit)}</name></author>" +
                    """<content type="html">${escapeHtml(content)}</content></entry>"""
            }
        val feedUrl = baseUrl + language.path(FEED_PATH)

        return xml(
            """<feed xmlns="http://www.w3.org/2005/Atom" xml:lang="${language.code}">""" +
                "<id>${escapeHtml(feedUrl)}</id>" +
                "<title>${escapeHtml("${HtmlPageRenderer.SITE_NAME} — ${texts.recentlyAdded}")}</title>" +
                "<subtitle>${escapeHtml(texts.homeDescription)}</subtitle>" +
                """<link rel="self" type="application/atom+xml" href="${escapeHtml(feedUrl)}"/>""" +
                """<link rel="alternate" type="text/html" href="${escapeHtml(baseUrl + language.path("/"))}"/>""" +
                "<updated>${atomDate(photos.firstOrNull()?.second?.reviewedAt)}</updated>" +
                entries +
                "</feed>",
            MediaType("application", "atom+xml", Charsets.UTF_8),
        )
    }

    private fun xml(
        body: String,
        contentType: MediaType = MediaType.APPLICATION_XML,
    ): ResponseEntity<String> =
        ResponseEntity
            .ok()
            .contentType(contentType)
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body("""<?xml version="1.0" encoding="UTF-8"?>$body""")

    private fun atomDate(date: ZonedDateTime?): String =
        (date ?: ZonedDateTime.now()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    companion object {
        const val VESSELS_PER_SITEMAP = 10_000
        const val FEED_PATH = "/feed.xml"
        private const val FEED_SIZE = 48

        /** Crawlers of AI search engines and assistants, and the tokens controlling AI training */
        internal val AI_CRAWLERS =
            listOf(
                // OpenAI: ChatGPT search, user-requested fetches, training
                "OAI-SearchBot",
                "ChatGPT-User",
                "GPTBot",
                // Anthropic: Claude search, user-requested fetches, training
                "Claude-SearchBot",
                "Claude-User",
                "ClaudeBot",
                // Perplexity
                "PerplexityBot",
                "Perplexity-User",
                // Google Gemini and Apple Intelligence (tokens read by Googlebot and Applebot)
                "Google-Extended",
                "Applebot-Extended",
                // Microsoft Copilot answers come from Bingbot; Meta AI, Mistral Le Chat, DuckDuckGo Assist
                "Meta-ExternalAgent",
                "MistralAI-User",
                "DuckAssistBot",
                // Common Crawl, which many models are trained on
                "CCBot",
            )
        private const val URLSET_OPEN =
            """<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" """ +
                """xmlns:image="http://www.google.com/schemas/sitemap-image/1.1" """ +
                """xmlns:xhtml="http://www.w3.org/1999/xhtml">"""
    }
}
