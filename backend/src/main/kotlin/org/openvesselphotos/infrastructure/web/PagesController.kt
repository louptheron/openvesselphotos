package org.openvesselphotos.infrastructure.web

import jakarta.servlet.http.HttpServletRequest
import org.openvesselphotos.domain.entities.Flags
import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Photo
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselFilter
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.public_api.GetRecentVessels
import org.openvesselphotos.domain.use_cases.public_api.GetVessel
import org.openvesselphotos.domain.use_cases.public_api.GetVesselsPage
import org.openvesselphotos.domain.use_cases.public_api.vesselFilter
import org.openvesselphotos.infrastructure.web.HtmlPageRenderer.Companion.SITE_NAME
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer

/**
 * Serves the pages of the React app, each with its own title, description, canonical URL, structured data and
 * server-rendered content, so that search engines index them without running JavaScript. Every public page is in
 * English at the root and in French under /fr (see [Language]).
 */
@Controller
class PagesController(
    private val htmlPageRenderer: HtmlPageRenderer,
    private val publicUrl: PublicUrl,
    private val getVessel: GetVessel,
    private val getRecentVessels: GetRecentVessels,
    private val getVesselsPage: GetVesselsPage,
) {
    @GetMapping("/", "/fr")
    fun home(
        @RequestParam(required = false) q: String?,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val language = Language.of(request)
        val texts = PageTexts.of(language)
        val baseUrl = publicUrl.get()
        val recentVessels = getRecentVessels.execute(24)
        val links =
            recentVessels.joinToString("") { (vessel, photos) ->
                """<li><a href="${language.path(vesselPath(vessel))}">${escapeHtml(vesselName(vessel))}</a> """ +
                    """(${texts.photos(photos.size)})</li>"""
            }

        return htmlPageRenderer.render(
            HtmlPage(
                title = texts.homeTitle,
                description = texts.homeDescription,
                canonicalPath = language.path("/"),
                language = language,
                pathInEveryLanguage = "/",
                // Search results are not worth indexing; the vessel pages are
                isIndexable = q.isNullOrBlank(),
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "WebSite",
                            "name" to SITE_NAME,
                            "url" to baseUrl + language.path("/"),
                            "description" to texts.homeDescription,
                            "inLanguage" to language.code,
                        ),
                    ),
                body =
                    """<main><h1>${escapeHtml(texts.homeHeading)}</h1>""" +
                        """<p>${escapeHtml(texts.homeDescription)}</p>""" +
                        """<h2>${escapeHtml(texts.recentlyAdded)}</h2><ul>$links</ul>""" +
                        """<p><a href="${language.path("/vessels")}">${escapeHtml(texts.allVessels)}</a> · """ +
                        """<a href="${language.path("/contribute")}">${escapeHtml(texts.contributePhoto)}</a> · """ +
                        """<a href="${language.path("/docs")}">${escapeHtml(texts.apiDocumentation)}</a></p></main>""",
            ),
        )
    }

    @GetMapping("/vessels/{keyType}/{key}", "/fr/vessels/{keyType}/{key}")
    fun vessel(
        @PathVariable keyType: String,
        @PathVariable key: String,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val language = Language.of(request)
        val texts = PageTexts.of(language)
        val details =
            try {
                getVessel.execute(VesselKey.of(KeyType.fromPathName(keyType), key))
            } catch (e: BackendUsageException) {
                return notFound(texts.noVesselYet("${keyType.uppercase()} ${key.uppercase()}"), language)
            }
        val vessel = details.vessel
        val photos = details.photos
        val baseUrl = publicUrl.get()
        val name = vesselName(vessel)
        val keys = listOfNotNull(vessel.cfr?.let { "CFR $it" }, vessel.mmsi?.let { "MMSI $it" }).joinToString(", ")
        val licences = photos.map { it.licence.label }.distinct().joinToString(", ")
        // e.g. "LO 752221, Lorient"
        val registration = listOfNotNull(vessel.registration, vessel.port).joinToString(", ").ifEmpty { null }
        val flagText = vessel.flag?.let { ", ${texts.flag(it)}" }.orEmpty()
        val description =
            texts.vesselDescription(
                name = name,
                details = "$keys${registration?.let { ", $it" }.orEmpty()}$flagText",
                photoCount = photos.size,
                licences = licences,
            )
        val vesselType = vessel.register.vesselType?.let { FaoCodes.vesselType(it, language) }
        // e.g. "LOLA (LR 539755) — 9 photos · Gillnetter · La Rochelle": people search a vessel by its name and the
        // marking painted on its hull, rather than by its CFR
        val pageTitle =
            listOfNotNull(
                vessel.name?.let { "$it (${vessel.registration ?: keys})" } ?: keys,
                " — ${photos.takeIf { it.isNotEmpty() }?.let { texts.photos(it.size) } ?: texts.vesselPhotos}",
                vesselType?.let { " · $it" },
                vessel.port?.let { " · $it" },
            ).joinToString("")
        val path = vesselPath(vessel)
        val canonicalPath = language.path(path)
        val contributeQuery = vessel.cfr?.let { "cfr=$it" } ?: "mmsi=${vessel.mmsi}"
        // Links each vessel page to others: search engines find and index them from there
        val related =
            details.related
                .takeIf { it.isNotEmpty() }
                ?.let { related ->
                    val samePort = vessel.port != null && related.all { it.vessel.port == vessel.port }
                    val title = if (samePort) texts.otherVesselsOf(vessel.port) else texts.otherVessels
                    val items =
                        related.joinToString("") { (other, otherPhotos) ->
                            vesselListItem(other, otherPhotos, language)
                        }

                    """<h2>${escapeHtml(title)}</h2><ul>$items</ul>"""
                }.orEmpty()

        val figures =
            photos.joinToString("") { photo ->
                val alt =
                    texts.photoAlt(
                        vessel = name + vessel.registration?.let { " ($it)" }.orEmpty(),
                        vesselType = vesselType,
                        place = photo.place,
                        credit = photo.credit,
                    )
                val caption =
                    listOfNotNull(
                        "© ${escapeHtml(photo.credit)}",
                        escapeHtml(photo.licence.label),
                        photo.takenOn?.let { escapeHtml(texts.taken(it)) },
                        photo.place?.let { escapeHtml(it) },
                        photo.sourceUrl?.let { """<a href="${escapeHtml(it)}">${escapeHtml(texts.source)}</a>""" },
                    ).joinToString(" · ") + photo.note?.let { "<br>${escapeHtml(it)}" }.orEmpty()

                """<figure><img src="${escapeHtml(photoUrl(baseUrl, vessel, photo, ImageSize.MEDIUM))}" """ +
                    """alt="${escapeHtml(alt)}" width="${ImageSize.MEDIUM.pixels}" """ +
                    """height="${photo.height * ImageSize.MEDIUM.pixels / photo.width}" />""" +
                    """<figcaption>$caption</figcaption></figure>"""
            }

        return htmlPageRenderer.render(
            HtmlPage(
                title = "$pageTitle | $SITE_NAME",
                description = description,
                canonicalPath = canonicalPath,
                language = language,
                pathInEveryLanguage = path,
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
                            "inLanguage" to language.code,
                            "primaryImageOfPage" to
                                photos.firstOrNull()?.let { imageObject(baseUrl, vessel, it, texts) },
                            "image" to photos.map { imageObject(baseUrl, vessel, it, texts) },
                        ).filterValues { it != null },
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "BreadcrumbList",
                            "itemListElement" to
                                listOf(
                                    mapOf(
                                        "@type" to "ListItem",
                                        "position" to 1,
                                        "name" to texts.search,
                                        "item" to baseUrl + language.path("/"),
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
                    """<main><p><a href="${language.path("/")}">${escapeHtml(texts.search)}</a> › """ +
                        """${escapeHtml(name)}</p><h1>${escapeHtml(name)}</h1>""" +
                        """<p>${escapeHtml(keys)}${registration?.let { " · ${escapeHtml(it)}" } ?: ""}""" +
                        """${vessel.flag?.let { " · ${escapeHtml(texts.flagFact(it))}" } ?: ""}</p>""" +
                        summary(vessel, vesselType, language)?.let { """<p>${escapeHtml(it)}</p>""" }.orEmpty() +
                        registerFacts(vessel, language)?.let { """<p>${escapeHtml(it)}</p>""" }.orEmpty() +
                        """<p>${escapeHtml(description)}</p>$figures$related""" +
                        // The contribution form is the same page for every vessel: not worth crawling
                        """<p><a href="${language.path("/contribute")}?$contributeQuery" rel="nofollow">""" +
                        """${escapeHtml(texts.addPhoto)}</a> · """ +
                        """<a href="${language.path("/vessels")}">${escapeHtml(texts.allVessels)}</a></p></main>""",
            ),
        )
    }

    /**
     * Every vessel with photos, by name and page by page: without these links, most vessel pages would only be known
     * from the sitemap, and search engines rarely index pages that no other page links to.
     */
    @GetMapping("/vessels", "/fr/vessels")
    fun vessels(
        @RequestParam(required = false) page: String?,
        @RequestParam(required = false) flag: String?,
        @RequestParam(required = false) port: String?,
        @RequestParam(required = false) vesselType: String?,
        @RequestParam(required = false) mainGear: String?,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val language = Language.of(request)
        val texts = PageTexts.of(language)
        val filter = vesselFilter(flag, port, vesselType, mainGear)
        val pageSize = GetVesselsPage.DIRECTORY_PAGE_SIZE
        val pageCount = getVesselsPage.pageCount(pageSize, filter).coerceAtLeast(1)
        if (page == "1") {
            return ResponseEntity
                .status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(language.path(directoryPath(1, filter))))
                .build()
        }
        val number = if (page == null) 1 else page.toIntOrNull()?.takeIf { it in 2..pageCount }
        if (number == null) {
            return notFound(texts.pageNotFound, language)
        }
        val vessels = getVesselsPage.execute(number, pageSize, filter)
        val total = getVesselsPage.count(filter)
        val pageText =
            if (pageCount > 1) {
                texts.pageRange(
                    number,
                    pageCount,
                    vessels.firstOrNull()?.let { vesselName(it.vessel) },
                    vessels.lastOrNull()?.let { vesselName(it.vessel) },
                )
            } else {
                ""
            }
        // e.g. "flag Norway, main gear Bottom otter trawls"
        val filterText =
            texts.filterFacts(
                country = filter.flag?.let { Flags.name(it, language.locale) },
                port = filter.port,
                vesselType = filter.vesselType?.let { FaoCodes.vesselType(it, language) ?: it },
                mainGear = filter.mainGear?.let { FaoCodes.gear(it, language) },
            )
        val heading = if (filter.isEmpty) texts.allVesselsWithPhotos else texts.vesselsWithPhotos(filterText)
        val description = texts.directoryDescription(total, pageText)
        val items = vessels.joinToString("") { (vessel, photos) -> vesselListItem(vessel, photos, language) }
        val previousLink =
            """<a href="${escapeHtml(language.path(directoryPath(number - 1, filter)))}" rel="prev">""" +
                """${escapeHtml(texts.previousPage)}</a>"""
        val nextLink =
            """<a href="${escapeHtml(language.path(directoryPath(number + 1, filter)))}" rel="next">""" +
                """${escapeHtml(texts.nextPage)}</a>"""
        val navigation =
            listOfNotNull(
                previousLink.takeIf { number > 1 },
                nextLink.takeIf { number < pageCount },
            ).joinToString(" · ")

        return htmlPageRenderer.render(
            HtmlPage(
                title = "$heading${if (pageCount > 1) " — ${texts.pageOf(number, pageCount)}" else ""} | $SITE_NAME",
                description = description,
                // Filtered pages repeat the directory's vessels: only the directory itself is indexed
                canonicalPath = language.path(directoryPath(number)).takeIf { filter.isEmpty },
                language = language,
                pathInEveryLanguage = directoryPath(number).takeIf { filter.isEmpty },
                isIndexable = filter.isEmpty,
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "CollectionPage",
                            "name" to heading,
                            "description" to description,
                            "url" to publicUrl.get() + language.path(directoryPath(number)),
                            "inLanguage" to language.code,
                        ),
                    ).takeIf { filter.isEmpty }
                        .orEmpty(),
                body =
                    """<main><h1>${escapeHtml(heading)}</h1><p>${escapeHtml(description)}</p>""" +
                        """<ul>$items</ul><p>$navigation</p></main>""",
            ),
        )
    }

    @GetMapping("/contribute", "/fr/contribute")
    fun contribute(request: HttpServletRequest): ResponseEntity<String> {
        val language = Language.of(request)
        val texts = PageTexts.of(language)

        return htmlPageRenderer.render(
            HtmlPage(
                title = "${texts.contributeTitle} | $SITE_NAME",
                description = texts.contributeDescription,
                canonicalPath = language.path("/contribute"),
                language = language,
                pathInEveryLanguage = "/contribute",
                body =
                    """<main><h1>${escapeHtml(texts.contributeHeading)}</h1>""" +
                        """<p>${escapeHtml(texts.contributeText)}</p></main>""",
            ),
        )
    }

    @GetMapping("/docs", "/fr/docs")
    fun docs(request: HttpServletRequest): ResponseEntity<String> {
        val language = Language.of(request)
        val texts = PageTexts.of(language)

        return htmlPageRenderer.render(
            HtmlPage(
                title = "${texts.docsTitle} | $SITE_NAME",
                description = texts.docsDescription,
                canonicalPath = language.path("/docs"),
                language = language,
                pathInEveryLanguage = "/docs",
                body =
                    """<main><h1>${escapeHtml(texts.docsHeading)}</h1><p>${escapeHtml(texts.docsText)} """ +
                        """<code>/v1/cfr/{cfr}.jpg</code>, <code>/v1/mmsi/{mmsi}.jpg</code>, <code>/v1/cfr/{cfr}</code> (JSON).</p></main>""",
            ),
        )
    }

    @GetMapping("/api-key", "/fr/api-key")
    fun apiKey(request: HttpServletRequest): ResponseEntity<String> {
        val language = Language.of(request)
        val texts = PageTexts.of(language)

        return htmlPageRenderer.render(
            HtmlPage(
                title = "${texts.apiKeyTitle} | $SITE_NAME",
                description = texts.apiKeyDescription,
                canonicalPath = language.path("/api-key"),
                language = language,
                pathInEveryLanguage = "/api-key",
                body =
                    """<main><h1>${escapeHtml(texts.apiKeyHeading)}</h1>""" +
                        """<p>${escapeHtml(texts.apiKeyText)}</p></main>""",
            ),
        )
    }

    @GetMapping("/admin", "/admin/stats", "/fr/admin", "/fr/admin/stats")
    fun admin(request: HttpServletRequest): ResponseEntity<String> =
        htmlPageRenderer.render(
            HtmlPage(
                title = "Review | $SITE_NAME",
                description = "Review queue.",
                canonicalPath = null,
                language = Language.of(request),
                isIndexable = false,
            ),
        )

    /** Any other single-segment path: the React app shows its "not found" page, with a real 404 status. */
    @GetMapping("/{path:[a-zA-Z0-9_-]+}", "/fr/{path:[a-zA-Z0-9_-]+}")
    fun other(request: HttpServletRequest): ResponseEntity<String> {
        val language = Language.of(request)

        return notFound(PageTexts.of(language).pageNotFound, language)
    }

    /** e.g. <li><a href="/vessels/cfr/FRA000752221">TALION</a> (CFR FRA000752221, MMSI 227000101, 2 photos)</li> */
    private fun vesselListItem(
        vessel: Vessel,
        photos: List<Photo>,
        language: Language,
    ): String {
        val details =
            listOfNotNull(
                vessel.cfr?.let { "CFR $it" },
                vessel.mmsi?.let { "MMSI $it" },
                vessel.port,
                PageTexts.of(language).photos(photos.size),
            ).joinToString(", ")

        return """<li><a href="${language.path(vesselPath(vessel))}">${escapeHtml(vesselName(vessel))}</a> """ +
            """(${escapeHtml(details)})</li>"""
    }

    /**
     * Gives each vessel page a sentence of its own, e.g. "LOLA is a gillnetter of 14.85 m, built in 1981, registered
     * in La Rochelle under the flag of France. Main gear: set gillnets." Null when nothing is known.
     */
    private fun summary(
        vessel: Vessel,
        vesselType: String?,
        language: Language,
    ): String? {
        val register = vessel.register
        val flag = vessel.flag ?: Flags.ofMmsi(vessel.mmsi)
        if (vesselType == null &&
            register.lengthMeters == null &&
            register.yearBuilt == null &&
            vessel.port == null &&
            flag == null
        ) {
            return null
        }

        return PageTexts.of(language).summary(
            name = vesselName(vessel),
            vesselType = vesselType,
            lengthMeters = register.lengthMeters,
            yearBuilt = register.yearBuilt,
            port = vessel.port,
            country = flag?.let { Flags.name(it, language.locale) },
            mainGear = register.mainGear?.let { FaoCodes.gear(it, language) },
        )
    }

    /** e.g. "Built in 1975 · 9.7 m · 73 kW · 5.77 GT · main gear Boat dredges (DRB) · call sign FO9789 (EU fleet register)" */
    private fun registerFacts(
        vessel: Vessel,
        language: Language,
    ): String? {
        val register = vessel.register

        return PageTexts.of(language).registerFacts(
            yearBuilt = register.yearBuilt,
            lengthMeters = register.lengthMeters,
            enginePowerKw = register.enginePowerKw,
            tonnageGt = register.tonnageGt,
            mainGear = register.mainGear?.let { "${FaoCodes.gear(it, language)} ($it)" },
            callSign = register.callSign,
        )
    }

    private fun notFound(
        title: String,
        language: Language,
    ): ResponseEntity<String> {
        val texts = PageTexts.of(language)

        return htmlPageRenderer.render(
            HtmlPage(
                title = "$title | $SITE_NAME",
                description = texts.homeDescription,
                canonicalPath = null,
                language = language,
                isIndexable = false,
                body =
                    """<main><h1>${escapeHtml(title)}</h1><p><a href="${language.path("/")}">""" +
                        """${escapeHtml(texts.searchVesselPhotos)}</a></p></main>""",
                status = HttpStatus.NOT_FOUND,
            ),
        )
    }

    private fun imageObject(
        baseUrl: String,
        vessel: Vessel,
        photo: Photo,
        texts: PageTexts,
    ): Map<String, Any?> =
        mapOf(
            "@type" to "ImageObject",
            "contentUrl" to photoUrl(baseUrl, vessel, photo, ImageSize.FULL),
            "thumbnailUrl" to photoUrl(baseUrl, vessel, photo, ImageSize.SMALL),
            "caption" to texts.photoCaption(vesselName(vessel), photo.credit),
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
        fun vesselName(vessel: Vessel): String = vessel.name ?: vessel.cfr?.let { "CFR $it" } ?: "MMSI ${vessel.mmsi}"

        private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)

        /** [page] starts at 1, whose URL has no page parameter. Same URLs as the React app's (VesselsPage). */
        fun directoryPath(
            page: Int,
            filter: VesselFilter = VesselFilter(),
        ): String {
            val parameters =
                listOfNotNull(
                    filter.flag?.let { "flag" to it },
                    filter.port?.let { "port" to it },
                    filter.vesselType?.let { "vesselType" to it },
                    filter.mainGear?.let { "mainGear" to it },
                    page.takeIf { it > 1 }?.let { "page" to "$it" },
                )
            val query = parameters.joinToString("&") { (name, value) -> "$name=${encode(value)}" }

            return "/vessels${if (query.isEmpty()) "" else "?$query"}"
        }

        /** A vessel's page is under its CFR when it has one, else its MMSI. In English: see [Language.path]. */
        fun vesselPath(vessel: Vessel): String =
            vessel.cfr?.let { "/vessels/cfr/$it" } ?: "/vessels/mmsi/${vessel.mmsi}"

        /**
         * Name of a vessel's photos in their URLs, e.g. "lola-lr-539755": search engines read the words of an image's
         * URL. Same as the React app's (getPhotoSlug).
         */
        fun photoSlug(vessel: Vessel): String =
            Normalizer
                .normalize(listOfNotNull(vessel.name, vessel.registration).joinToString(" "), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .take(MAX_SLUG_LENGTH)
                .trim('-')
                .ifEmpty { "vessel" }

        /** e.g. "https://openvesselphotos.org/v1/cfr/FRA000539755/photos/1/lola-lr-539755.jpg?size=medium" */
        fun photoUrl(
            baseUrl: String,
            vessel: Vessel,
            photo: Photo,
            size: ImageSize,
        ): String {
            val (type, key) = vessel.cfr?.let { KeyType.CFR to it } ?: (KeyType.MMSI to vessel.mmsi)

            return "$baseUrl/v1/${type.pathName}/$key/photos/${photo.number}/${photoSlug(
                vessel,
            )}.jpg?size=${size.param}"
        }

        private const val MAX_SLUG_LENGTH = 80
    }
}
