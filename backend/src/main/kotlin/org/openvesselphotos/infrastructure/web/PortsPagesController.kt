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
import java.text.NumberFormat
import java.time.Duration
import java.util.Locale

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
        val auctions = ports.mapNotNull { it.auction }.sortedByDescending { it.valueThousandEuros }
        val description =
            "Carte de la pêche française : ${number(vesselCount)} navires de pêche dans ${ports.size} ports, " +
                "leurs engins et leur taille, et les ventes de ${auctions.size} criées (espèces, prix et saisons)."
        val portItems =
            ports.sortedByDescending { it.vesselCount }.joinToString("") { (port, count, _, auction) ->
                """<li><a href="/${port.locode}">${escapeHtml(port.name)}</a> : ${vessels(count)}""" +
                    (auction?.let { ", criée ${escapeHtml(it.auction)}" } ?: "") + "</li>"
            }
        val auctionItems =
            auctions.joinToString("") {
                """<li><a href="/${it.portLocode}">${escapeHtml(it.auction)}</a> : """ +
                    "${number(it.quantityTonnes)} t vendues en ${it.year}, ${millions(it.valueThousandEuros)}</li>"
            }

        return render(
            HtmlPage(
                title = "${Site.PORTS.siteName} : la flotte, les ports et les criées",
                description = description,
                canonicalPath = "/",
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "WebSite",
                            "name" to Site.PORTS.siteName,
                            "alternateName" to "Ports de pêche de France",
                            "url" to publicUrl.get(Site.PORTS) + "/",
                            "description" to description,
                            "inLanguage" to "fr",
                        ),
                    ),
                body =
                    "<main><h1>La pêche française, port par port</h1>" +
                        "<p>${escapeHtml(description)}</p>" +
                        "<p>Chaque navire est rattaché au port où il a été le plus souvent observé à quai. " +
                        "Pour chaque port : sa flotte par taille et par engin de pêche (chalut, filets, lignes, " +
                        "casiers, dragues…), ses navires et leurs photos, et ce que vend sa criée.</p>" +
                        "<h2>Les ports de pêche</h2><ul>$portItems</ul>" +
                        (if (auctionItems.isEmpty()) "" else "<h2>Les criées</h2><ul>$auctionItems</ul>") +
                        "</main>",
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
        val ofPort = PageTexts.French.of(port.name)
        val registers = details.vessels.mapNotNull { it.vessel?.vessel?.register }
        val gears =
            GEAR_FAMILIES
                .map { (label, codes) -> label to registers.count { it.mainGear in codes } }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
        val lengths = registers.mapNotNull { it.lengthMeters }
        val sizes =
            listOf(
                "petite pêche, moins de 12 m" to lengths.count { it < 12 },
                "pêche au large, 12 à 24 m" to lengths.count { it >= 12 && it < 24 },
                "pêche industrielle, 24 m et plus" to lengths.count { it >= 24 },
            ).filter { it.second > 0 }
        val auction = details.auction
        val auctionTotal = auction?.sales?.firstOrNull { it.year == auction.year && it.species == null }
        val species =
            auction
                ?.sales
                ?.filter {
                    it.year == auction.year &&
                        it.species != null &&
                        !it.species.startsWith("autre", ignoreCase = true)
                }?.sortedByDescending { it.valueThousandEuros }
                .orEmpty()

        val title =
            "Port de pêche $ofPort : ${vessels(details.vessels.size)}" + (if (auction != null) " et sa criée" else "")
        val description =
            buildString {
                append("${vessels(details.vessels.size).replaceFirstChar { it.uppercase() }} de pêche ")
                append("basé${if (details.vessels.size > 1) "s" else ""} au port $ofPort")
                if (gears.isNotEmpty()) {
                    append(" : ")
                    append(gears.take(3).joinToString(", ") { (label, count) -> "${label.lowercase()} ($count)" })
                }
                append(".")
                if (auctionTotal != null) {
                    append(" Criée : ${number(auctionTotal.quantityTonnes)} t vendues en ${auction.year}")
                    append(" pour ${millions(auctionTotal.valueThousandEuros)}")
                    if (species.isNotEmpty()) {
                        append(" (${species.take(3).joinToString(", ") { it.species!!.lowercase() }})")
                    }
                    append(".")
                }
            }

        val vesselsBaseUrl = publicUrl.get()
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
                val facts =
                    listOfNotNull(
                        vessel?.register?.lengthMeters?.let { "${decimal(it)} m" },
                        vessel?.register?.mainGear?.let { gearLabel(it) },
                        "CFR ${escapeHtml(homePort.cfr)}",
                    )

                "<li>$link (${facts.joinToString(", ")})</li>"
            }
        val fleet =
            buildString {
                append("<h2>La flotte du port</h2>")
                if (sizes.isNotEmpty()) {
                    append("<h3>Taille des navires</h3><ul>")
                    sizes.forEach { (label, count) -> append("<li>${vessels(count)} : $label</li>") }
                    append("</ul>")
                }
                if (gears.isNotEmpty()) {
                    append("<h3>Engins de pêche</h3><ul>")
                    gears.forEach { (label, count) -> append("<li>$label : ${vessels(count)}</li>") }
                    append("</ul>")
                }
            }
        val auctionSection =
            if (auction == null || auctionTotal == null) {
                ""
            } else {
                "<h2>Criée ${escapeHtml(PageTexts.French.of(auction.name))}</h2>" +
                    "<p>${number(auctionTotal.quantityTonnes)} t de produits de la mer vendues en ${auction.year}, " +
                    "pour ${millions(auctionTotal.valueThousandEuros)}" +
                    (auctionTotal.priceEurosPerKg?.let { ", soit ${price(it)} en moyenne" } ?: "") +
                    ". Source : FranceAgriMer.</p>" +
                    "<h3>Principales espèces</h3><ul>" +
                    species.joinToString("") {
                        "<li>${escapeHtml(it.species!!.replaceFirstChar { c -> c.uppercase() })} : " +
                            "${number(it.quantityTonnes)} t" + (
                                it.priceEurosPerKg?.let { p ->
                                    ", ${price(p)}"
                                } ?: ""
                            ) +
                            "</li>"
                    } + "</ul>"
            }
        val mainPhoto =
            details.vessels.firstNotNullOfOrNull { portVessel ->
                portVessel.vessel?.let { (vessel, photos) ->
                    photos.firstOrNull()?.let {
                        PagesController.photoUrl(vesselsBaseUrl, vessel, it, ImageSize.FULL)
                    }
                }
            }
        val portUrl = publicUrl.get(Site.PORTS) + "/${port.locode}"

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
                            "name" to "Port de pêche $ofPort",
                            "description" to description,
                            "url" to portUrl,
                            "address" to
                                mapOf(
                                    "@type" to "PostalAddress",
                                    "addressLocality" to port.name,
                                    "addressCountry" to port.country,
                                ),
                            "geo" to
                                port.latitude?.let {
                                    mapOf("@type" to "GeoCoordinates", "latitude" to it, "longitude" to port.longitude)
                                },
                        ).filterValues { it != null },
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "BreadcrumbList",
                            "itemListElement" to
                                listOf(
                                    mapOf(
                                        "@type" to "ListItem",
                                        "position" to 1,
                                        "name" to Site.PORTS.siteName,
                                        "item" to publicUrl.get(Site.PORTS) + "/",
                                    ),
                                    mapOf(
                                        "@type" to "ListItem",
                                        "position" to 2,
                                        "name" to port.name,
                                        "item" to portUrl,
                                    ),
                                ),
                        ),
                    ),
                body =
                    """<main><nav><a href="/">${Site.PORTS.siteName}</a> › ${escapeHtml(port.name)}</nav>""" +
                        "<h1>${escapeHtml("Port de pêche $ofPort")}</h1><p>${escapeHtml(description)}</p>" +
                        fleet + auctionSection +
                        "<h2>Les navires</h2><ul>$items</ul></main>",
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
                body =
                    "<main><h1>Page introuvable</h1>" +
                        """<p><a href="/">${Site.PORTS.siteName} : tous les ports</a></p></main>""",
                status = HttpStatus.NOT_FOUND,
            ),
        )

    private fun render(page: HtmlPage) = htmlPageRenderer.render(page.copy(language = Language.FR, site = Site.PORTS))

    private fun vessels(count: Int) = if (count == 1) "1 navire" else "${number(count)} navires"

    companion object {
        private val FRENCH = Locale.FRANCE

        /** The families of gears of the map, by FAO code (ISSCFG), as in frontend/src/ports/gears.tsx */
        val GEAR_FAMILIES =
            listOf(
                "Chalut de fond" to setOf("OTB", "OTT", "PTB", "TBB", "TBN", "TBS", "OT", "TB"),
                "Senne démersale" to setOf("SDN", "SSC", "SPR", "SX"),
                "Chalut pélagique" to setOf("OTM", "PTM", "TM", "TMS"),
                "Dragues" to setOf("DRB", "DRH", "DRM", "DRX", "HMD"),
                "Senne tournante" to setOf("PS", "PS1", "PS2", "LA", "SB", "SV"),
                "Filets" to setOf("GNS", "GND", "GNC", "GNF", "GTR", "GTN", "GN", "GEN"),
                "Palangres" to setOf("LLS", "LLD", "LL"),
                "Lignes" to setOf("LHP", "LHM", "LTL", "LX", "LVT"),
                "Casiers" to setOf("FPO", "FIX", "FPN", "FYK", "FWR"),
            )

        fun gearLabel(code: String): String? = GEAR_FAMILIES.firstOrNull { code in it.second }?.first?.lowercase()

        /** e.g. "12 345", with a narrow no-break space */
        fun number(value: Number): String = NumberFormat.getIntegerInstance(FRENCH).format(Math.round(value.toDouble()))

        /** e.g. "12,7" */
        fun decimal(value: Double): String =
            NumberFormat.getNumberInstance(FRENCH).apply { maximumFractionDigits = 1 }.format(value)

        /** e.g. "43,4 M€", "830 k€" */
        fun millions(thousandEuros: Double): String =
            if (thousandEuros >= 1000) "${decimal(thousandEuros / 1000)} M€" else "${number(thousandEuros)} k€"

        /** e.g. "6,19 €/kg" */
        fun price(eurosPerKg: Double): String =
            NumberFormat
                .getNumberInstance(FRENCH)
                .apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }.format(eurosPerKg) + " €/kg"
    }
}
