package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.public_api.GetPort
import org.openvesselphotos.domain.use_cases.public_api.GetPorts
import org.openvesselphotos.infrastructure.web.SiteHostFilter.Companion.PORTS_PREFIX
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.net.URI
import java.time.Duration

/**
 * The pages of the ports site, in French, reached on its own host only (see [SiteHostFilter], which forwards "/FRQUY"
 * to "/_ports/FRQUY"). As for Open Vessel Photos, each page is rendered with its metadata and content for search
 * engines, then the React app (frontend/ports.html) takes over. Vessel pages and photos link to Open Vessel Photos.
 */
@Controller
class PortsPagesController(
    private val htmlPageRenderer: HtmlPageRenderer,
    private val publicUrl: PublicUrl,
    private val getPorts: GetPorts,
    private val getPort: GetPort,
) {
    @GetMapping(PORTS_PREFIX, "$PORTS_PREFIX/")
    fun map(): ResponseEntity<String> {
        val ports = getPorts.execute()
        val vesselCount = ports.sumOf { it.vesselCount }
        val description =
            "Carte des $vesselCount navires de pêche français par port d’attache, dans ${ports.size} ports : " +
                "leurs photos, leur type et leur longueur."
        val links =
            ports.joinToString("") { (port, count) ->
                """<li><a href="/${port.locode}">${escapeHtml(port.name)}</a> (${vessels(count)})</li>"""
            }

        return render(
            HtmlPage(
                title = "Les navires de pêche français par port | ${Site.PORTS.siteName}",
                description = description,
                canonicalPath = "/",
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "WebSite",
                            "name" to Site.PORTS.siteName,
                            "url" to publicUrl.get(Site.PORTS) + "/",
                            "description" to description,
                            "inLanguage" to "fr",
                        ),
                    ),
                body =
                    "<main><h1>Les navires de pêche français par port</h1>" +
                        "<p>${escapeHtml(description)}</p><ul>$links</ul></main>",
            ),
        )
    }

    @GetMapping("$PORTS_PREFIX/{locode:[A-Za-z]{2}[A-Za-z0-9]{3}(?:-[0-9]{1,4})?}")
    fun port(
        @PathVariable locode: String,
    ): ResponseEntity<String> {
        if (locode != locode.uppercase()) {
            return ResponseEntity
                .status(
                    HttpStatus.MOVED_PERMANENTLY,
                ).location(URI.create("/${locode.uppercase()}"))
                .build()
        }
        val details =
            try {
                getPort.execute(locode)
            } catch (_: BackendUsageException) {
                return notFound()
            }
        val port = details.port
        val vesselsBaseUrl = publicUrl.get()
        val description =
            "${vessels(details.vessels.size).replaceFirstChar { it.uppercase() }} de pêche " +
                "basé${if (details.vessels.size > 1) "s" else ""} au port " +
                "${PageTexts.French.of(port.name)} (${port.locode}), avec leurs photos, leur type et leur longueur."
        val title = "Navires de pêche du port ${PageTexts.French.of(port.name)}"
        val items =
            details.vessels.joinToString("") { (homePort, vesselWithPhotos) ->
                val vessel = vesselWithPhotos?.vessel
                val name = escapeHtml(vessel?.name ?: homePort.vesselName ?: "CFR ${homePort.cfr}")
                val link =
                    vessel?.let {
                        """<a href="${escapeHtml(
                            vesselsBaseUrl + PagesController.vesselPath(it),
                        )}">$name</a>"""
                    }
                        ?: name

                "<li>$link (CFR ${escapeHtml(homePort.cfr)})</li>"
            }
        val mainPhoto =
            details.vessels.firstNotNullOfOrNull { portVessel ->
                portVessel.vessel?.let { (vessel, photos) ->
                    photos.firstOrNull()?.let {
                        PagesController.photoUrl(vesselsBaseUrl, vessel, it, ImageSize.FULL)
                    }
                }
            }

        return render(
            HtmlPage(
                title = "$title | ${Site.PORTS.siteName}",
                description = description,
                canonicalPath = "/${port.locode}",
                imageUrl = mainPhoto,
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "Place",
                            "name" to "Port ${PageTexts.French.of(port.name)}",
                            "url" to publicUrl.get(Site.PORTS) + "/${port.locode}",
                            "geo" to
                                port.latitude?.let {
                                    mapOf("@type" to "GeoCoordinates", "latitude" to it, "longitude" to port.longitude)
                                },
                        ).filterValues { it != null },
                    ),
                body =
                    """<main><p><a href="/">Tous les ports</a></p>""" +
                        "<h1>${escapeHtml(title)}</h1><p>${escapeHtml(description)}</p><ul>$items</ul></main>",
            ),
        )
    }

    /** Every crawler is welcome, AI ones too (see [CrawlersController.robots]); the shared API is not to be indexed. */
    @GetMapping("$PORTS_PREFIX/robots.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    @ResponseBody
    fun robots(): String {
        val rules = "Allow: /\nDisallow: /v1/"

        return "User-agent: *\n$rules\n\n" +
            CrawlersController.AI_CRAWLERS.joinToString("\n") { "User-agent: $it" } + "\n$rules\n\n" +
            "Sitemap: ${publicUrl.get(Site.PORTS)}/sitemap.xml\n"
    }

    @GetMapping("$PORTS_PREFIX/sitemap.xml")
    @ResponseBody
    fun sitemap(): ResponseEntity<String> {
        val baseUrl = publicUrl.get(Site.PORTS)
        val urls =
            (listOf("/") + getPorts.execute().map { "/${it.port.locode}" })
                .joinToString("") { "<url><loc>${escapeHtml(baseUrl + it)}</loc></url>" }

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_XML)
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(
                """<?xml version="1.0" encoding="UTF-8"?>""" +
                    """<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">$urls</urlset>""",
            )
    }

    /** Any other page of the ports host, e.g. a page of Open Vessel Photos */
    @GetMapping("$PORTS_PREFIX/**")
    fun notFound(): ResponseEntity<String> =
        render(
            HtmlPage(
                title = "Page introuvable | ${Site.PORTS.siteName}",
                description = "Cette page n’existe pas.",
                canonicalPath = null,
                isIndexable = false,
                body = """<main><h1>Page introuvable</h1><p><a href="/">Tous les ports</a></p></main>""",
                status = HttpStatus.NOT_FOUND,
            ),
        )

    private fun render(page: HtmlPage) = htmlPageRenderer.render(page.copy(language = Language.FR, site = Site.PORTS))

    private fun vessels(count: Int) = if (count == 1) "1 navire" else "$count navires"
}
