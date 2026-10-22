package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.public_api.GetRecentVessels
import org.openvesselphotos.domain.use_cases.public_api.GetVessel
import org.openvesselphotos.domain.use_cases.public_api.GetVesselsPage
import org.openvesselphotos.infrastructure.web.HtmlPageRenderer.Companion.SITE_NAME
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.net.URI

/**
 * Serves the pages of the React app, each with its own title, description, canonical URL, structured data and
 * server-rendered content, so that search engines index them without running JavaScript.
 */
@Controller
class PagesController(
    private val htmlPageRenderer: HtmlPageRenderer,
    private val publicUrl: PublicUrl,
    private val getVessel: GetVessel,
    private val getRecentVessels: GetRecentVessels,
    private val getVesselsPage: GetVesselsPage,
) {
    @GetMapping("/")
    fun home(
        @RequestParam(required = false) q: String?,
    ): ResponseEntity<String> {
        val baseUrl = publicUrl.get()
        val recentVessels = getRecentVessels.execute(24)
        val links =
            recentVessels.joinToString("") { (vessel, photos) ->
                """<li><a href="${vesselPath(
                    vessel,
                )}">${escapeHtml(
                    vesselName(vessel),
                )}</a> (${photos.size} photo${if (photos.size == 1) "" else "s"})</li>"""
            }

        return htmlPageRenderer.render(
            HtmlPage(
                title = "$SITE_NAME — free photos of any vessel, by CFR or MMSI",
                description = HOME_DESCRIPTION,
                canonicalPath = "/",
                // Search results are not worth indexing; the vessel pages are
                isIndexable = q.isNullOrBlank(),
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "WebSite",
                            "name" to SITE_NAME,
                            "url" to "$baseUrl/",
                            "description" to HOME_DESCRIPTION,
                        ),
                    ),
                body =
                    """<main><h1>The open library of vessel photos, by CFR or MMSI.</h1>""" +
                        """<p>${escapeHtml(HOME_DESCRIPTION)}</p>""" +
                        """<h2>Recently added</h2><ul>$links</ul>""" +
                        """<p><a href="/vessels">All vessels</a> · <a href="/contribute">Contribute a photo</a> · """ +
                        """<a href="/docs">API documentation</a></p></main>""",
            ),
        )
    }

    @GetMapping("/vessels/{keyType}/{key}")
    fun vessel(
        @PathVariable keyType: String,
        @PathVariable key: String,
    ): ResponseEntity<String> {
        val details =
            try {
                getVessel.execute(VesselKey.of(KeyType.fromPathName(keyType), key))
            } catch (e: BackendUsageException) {
                return notFound("No vessel with ${keyType.uppercase()} ${key.uppercase()} yet")
            }
        val vessel = details.vessel
        val photos = details.photos
        val baseUrl = publicUrl.get()
        val name = vesselName(vessel)
        val keys = listOfNotNull(vessel.cfr?.let { "CFR $it" }, vessel.mmsi?.let { "MMSI $it" }).joinToString(", ")
        val licences = photos.map { it.licence.label }.distinct().joinToString(", ")
        // e.g. "LO 752221, Lorient"
        val registration = listOfNotNull(vessel.registration, vessel.port).joinToString(", ").ifEmpty { null }
        val registrationText = registration?.let { ", $it" } ?: ""
        val description =
            when {
                photos.isEmpty() ->
                    "Vessel $name ($keys$registrationText)${vessel.flag?.let { ", flag $it" } ?: ""}. " +
                        "No photo yet: contribute the first one."
                else ->
                    "${photos.size} photo${if (photos.size == 1) "" else "s"} " +
                        "of the vessel $name ($keys$registrationText)" +
                        "${vessel.flag?.let { ", flag $it" } ?: ""}, free to reuse under open licences ($licences)."
            }
        val canonicalPath = vesselPath(vessel)
        val contributeQuery = vessel.cfr?.let { "cfr=$it" } ?: "mmsi=${vessel.mmsi}"
        // Links each vessel page to others: search engines find and index them from there
        val related =
            details.related
                .takeIf { it.isNotEmpty() }
                ?.let { related ->
                    val samePort = vessel.port != null && related.all { it.vessel.port == vessel.port }
                    val title = if (samePort) "Other vessels of ${vessel.port}" else "Other vessels"
                    val items = related.joinToString("") { (other, otherPhotos) -> vesselListItem(other, otherPhotos) }

                    """<h2>${escapeHtml(title)}</h2><ul>$items</ul>"""
                }.orEmpty()

        val figures =
            photos.joinToString("") { photo ->
                """<figure><img src="${escapeHtml(photoUrl(baseUrl, vessel, photo, ImageSize.MEDIUM))}" """ +
                    """alt="${escapeHtml("$name, by ${photo.credit}")}" width="${ImageSize.MEDIUM.pixels}" """ +
                    """height="${photo.height * ImageSize.MEDIUM.pixels / photo.width}" />""" +
                    """<figcaption>© ${escapeHtml(photo.credit)} · ${escapeHtml(photo.licence.label)}""" +
                    """${photo.takenOn?.let { " · taken $it" } ?: ""}${photo.place?.let {
                        " · ${escapeHtml(
                            it,
                        )}"
                    } ?: ""}${photo.note?.let { "<br>${escapeHtml(it)}" } ?: ""}</figcaption></figure>"""
            }

        return htmlPageRenderer.render(
            HtmlPage(
                title = "$name ($keys) — vessel photos | $SITE_NAME",
                description = description,
                canonicalPath = canonicalPath,
                // Pages without photo are near-identical: indexed once their first photo is approved
                isIndexable = photos.isNotEmpty(),
                imageUrl = photos.firstOrNull()?.let { photoUrl(baseUrl, vessel, it, ImageSize.MEDIUM) },
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "WebPage",
                            "name" to "$name ($keys)",
                            "description" to description,
                            "url" to baseUrl + canonicalPath,
                            "primaryImageOfPage" to photos.firstOrNull()?.let { imageObject(baseUrl, vessel, it) },
                            "image" to photos.map { imageObject(baseUrl, vessel, it) },
                        ).filterValues { it != null },
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "BreadcrumbList",
                            "itemListElement" to
                                listOf(
                                    mapOf(
                                        "@type" to "ListItem",
                                        "position" to 1,
                                        "name" to "Search",
                                        "item" to "$baseUrl/",
                                    ),
                                    mapOf(
                                        "@type" to "ListItem",
                                        "position" to 2,
                                        "name" to name,
                                        "item" to baseUrl + canonicalPath,
                                    ),
                                ),
                        ),
                    ),
                body =
                    """<main><p><a href="/">Search</a> › ${escapeHtml(name)}</p><h1>${escapeHtml(name)}</h1>""" +
                        """<p>${escapeHtml(keys)}${registration?.let { " · ${escapeHtml(it)}" } ?: ""}""" +
                        """${vessel.flag?.let { " · Flag ${escapeHtml(it)}" } ?: ""}</p>""" +
                        registerFacts(vessel)?.let { """<p>${escapeHtml(it)}</p>""" }.orEmpty() +
                        """<p>${escapeHtml(description)}</p>$figures$related""" +
                        // The contribution form is the same page for every vessel: not worth crawling
                        """<p><a href="/contribute?$contributeQuery" rel="nofollow">Add a photo</a> · """ +
                        """<a href="/vessels">All vessels</a></p></main>""",
            ),
        )
    }

    /**
     * Every vessel with photos, by name and page by page: without these links, most vessel pages would only be known
     * from the sitemap, and search engines rarely index pages that no other page links to.
     */
    @GetMapping("/vessels")
    fun vessels(
        @RequestParam(required = false) page: String?,
    ): ResponseEntity<String> {
        val pageSize = GetVesselsPage.DIRECTORY_PAGE_SIZE
        val pageCount = getVesselsPage.pageCount(pageSize).coerceAtLeast(1)
        if (page == "1") {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(directoryPath(1))).build()
        }
        val number = if (page == null) 1 else page.toIntOrNull()?.takeIf { it in 2..pageCount }
        if (number == null) {
            return notFound("Page not found")
        }
        val vessels = getVesselsPage.execute(number, pageSize)
        val total = getVesselsPage.count()
        val range =
            vessels
                .takeIf { it.isNotEmpty() }
                ?.let { " (${vesselName(it.first().vessel)} to ${vesselName(it.last().vessel)})" }
                .orEmpty()
        val pageText = if (pageCount > 1) ", page $number of $pageCount$range" else ""
        val description = "Browse the $total vessels with free photos on $SITE_NAME, by name$pageText."
        val items = vessels.joinToString("") { (vessel, photos) -> vesselListItem(vessel, photos) }
        val previousLink = """<a href="${directoryPath(number - 1)}" rel="prev">Previous page</a>"""
        val nextLink = """<a href="${directoryPath(number + 1)}" rel="next">Next page</a>"""
        val navigation =
            listOfNotNull(
                previousLink.takeIf { number > 1 },
                nextLink.takeIf { number < pageCount },
            ).joinToString(" · ")

        return htmlPageRenderer.render(
            HtmlPage(
                title =
                    "All vessels with photos${if (pageCount > 1) " — page $number of $pageCount" else ""} | $SITE_NAME",
                description = description,
                canonicalPath = directoryPath(number),
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "CollectionPage",
                            "name" to "All vessels with photos",
                            "description" to description,
                            "url" to publicUrl.get() + directoryPath(number),
                        ),
                    ),
                body =
                    """<main><h1>All vessels with photos</h1><p>${escapeHtml(description)}</p>""" +
                        """<ul>$items</ul><p>$navigation</p></main>""",
            ),
        )
    }

    @GetMapping("/contribute")
    fun contribute(): ResponseEntity<String> =
        htmlPageRenderer.render(
            HtmlPage(
                title = "Contribute a vessel photo | $SITE_NAME",
                description =
                    "Share your own photo of a vessel under an open licence (CC BY, CC BY-SA or CC0), " +
                        "with its CFR or MMSI. " +
                        "Published once reviewed by a volunteer.",
                canonicalPath = "/contribute",
                body =
                    """<main><h1>Contribute a photo</h1>""" +
                        """<p>Your own picture, an open licence, and the vessel's CFR or MMSI.</p></main>""",
            ),
        )

    @GetMapping("/docs")
    fun docs(): ResponseEntity<String> =
        htmlPageRenderer.render(
            HtmlPage(
                title = "Free vessel photo API, by CFR or MMSI | $SITE_NAME",
                description =
                    "Free JSON and image API for vessel photos: one URL per vessel, by CFR or MMSI, " +
                        "with credit and licence. " +
                        "No API key, CORS enabled.",
                canonicalPath = "/docs",
                body =
                    """<main><h1>The vessel photo API</h1><p>CFR and MMSI are both first-class keys: """ +
                        """<code>/v1/cfr/{cfr}.jpg</code>, <code>/v1/mmsi/{mmsi}.jpg</code>, <code>/v1/cfr/{cfr}</code> (JSON).</p></main>""",
            ),
        )

    @GetMapping("/api-key")
    fun apiKey(): ResponseEntity<String> =
        htmlPageRenderer.render(
            HtmlPage(
                title = "Request an API key | $SITE_NAME",
                description =
                    "Request a free API key to contribute vessel photos, suggest CFR or MMSI keys and report photos " +
                        "with scripts. Reading the API needs no key.",
                canonicalPath = "/api-key",
                body =
                    """<main><h1>Request an API key</h1>""" +
                        """<p>Reading the API is free and open. An API key is only needed to send data with a script.</p></main>""",
            ),
        )

    @GetMapping("/admin", "/admin/stats")
    fun admin(): ResponseEntity<String> =
        htmlPageRenderer.render(
            HtmlPage(
                title = "Review | $SITE_NAME",
                description = "Review queue.",
                canonicalPath = null,
                isIndexable = false,
            ),
        )

    /** Any other single-segment path: the React app shows its "not found" page, with a real 404 status. */
    @GetMapping("/{path:[a-zA-Z0-9_-]+}")
    fun other(): ResponseEntity<String> = notFound("Page not found")

    /** e.g. <li><a href="/vessels/cfr/FRA000752221">TALION</a> (CFR FRA000752221, MMSI 227000101, 2 photos)</li> */
    private fun vesselListItem(
        vessel: Vessel,
        photos: List<Photo>,
    ): String {
        val details =
            listOfNotNull(
                vessel.cfr?.let { "CFR $it" },
                vessel.mmsi?.let { "MMSI $it" },
                vessel.port,
                "${photos.size} photo${if (photos.size == 1) "" else "s"}",
            ).joinToString(", ")

        return """<li><a href="${vesselPath(
            vessel,
        )}">${escapeHtml(vesselName(vessel))}</a> (${escapeHtml(details)})</li>"""
    }

    /** e.g. "Built in 1975 · 9.7 m · 73 kW · 5.77 GT · main gear DRB · call sign FO9789 (EU fleet register)" */
    private fun registerFacts(vessel: Vessel): String? {
        val register = vessel.register
        val facts =
            listOfNotNull(
                register.yearBuilt?.let { "Built in $it" },
                register.lengthMeters?.let { "${formatNumber(it)} m" },
                register.enginePowerKw?.let { "${formatNumber(it)} kW" },
                register.tonnageGt?.let { "${formatNumber(it)} GT" },
                register.mainGear?.let { "main gear $it" },
                register.callSign?.let { "call sign $it" },
            )

        return facts.takeIf { it.isNotEmpty() }?.joinToString(" · ", postfix = " (EU fleet register)")
    }

    private fun formatNumber(value: Double): String = value.toBigDecimal().stripTrailingZeros().toPlainString()

    private fun notFound(title: String): ResponseEntity<String> =
        htmlPageRenderer.render(
            HtmlPage(
                title = "$title | $SITE_NAME",
                description = HOME_DESCRIPTION,
                canonicalPath = null,
                isIndexable = false,
                body = """<main><h1>${escapeHtml(title)}</h1><p><a href="/">Search vessel photos</a></p></main>""",
                status = HttpStatus.NOT_FOUND,
            ),
        )

    private fun imageObject(
        baseUrl: String,
        vessel: Vessel,
        photo: Photo,
    ): Map<String, Any?> =
        mapOf(
            "@type" to "ImageObject",
            "contentUrl" to photoUrl(baseUrl, vessel, photo, ImageSize.FULL),
            "thumbnailUrl" to photoUrl(baseUrl, vessel, photo, ImageSize.SMALL),
            "caption" to "${vesselName(vessel)}, by ${photo.credit}",
            "width" to photo.width,
            "height" to photo.height,
            "license" to photo.licence.url,
            "acquireLicensePage" to "$baseUrl/docs#licences",
            "creditText" to photo.credit,
            "creator" to mapOf("@type" to "Person", "name" to photo.credit),
            "copyrightNotice" to photo.credit,
            "dateCreated" to photo.takenOn?.toString(),
            "contentLocation" to photo.place?.let { mapOf("@type" to "Place", "name" to it) },
        ).filterValues { it != null }

    companion object {
        const val HOME_DESCRIPTION =
            "Free, community-built library of vessel photos, searchable by CFR or MMSI. " +
                "Every photo comes with its credit and an open licence, and a free API."

        fun vesselName(vessel: Vessel): String = vessel.name ?: vessel.cfr?.let { "CFR $it" } ?: "MMSI ${vessel.mmsi}"

        /** [page] starts at 1, whose URL has no parameter. */
        fun directoryPath(page: Int): String = if (page <= 1) "/vessels" else "/vessels?page=$page"

        /** A vessel's page is under its CFR when it has one, else its MMSI. */
        fun vesselPath(vessel: Vessel): String =
            vessel.cfr?.let { "/vessels/cfr/$it" } ?: "/vessels/mmsi/${vessel.mmsi}"

        fun photoUrl(
            baseUrl: String,
            vessel: Vessel,
            photo: Photo,
            size: ImageSize,
        ): String {
            val (type, key) = vessel.cfr?.let { KeyType.CFR to it } ?: (KeyType.MMSI to vessel.mmsi)

            return "$baseUrl/v1/${type.pathName}/$key/photos/${photo.number}.jpg?size=${size.param}"
        }
    }
}
