package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.entities.HomePort
import org.openvesselphotos.domain.entities.HomePortSource
import org.openvesselphotos.domain.entities.PortWithVesselCount
import org.openvesselphotos.domain.entities.SpeciesSales
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.admin.ImportAuctionSales
import org.openvesselphotos.domain.use_cases.public_api.GetPort
import org.openvesselphotos.domain.use_cases.public_api.GetPorts
import org.openvesselphotos.domain.use_cases.public_api.GetSpecies
import org.openvesselphotos.infrastructure.web.PortsSeo.Question
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
 * The pages of the ports site, in French, reached on its own host only (see [SiteHostFilter], which forwards
 * "/ports/erquy" to "/_ports/ports/erquy"). As for Open Vessel Photos, each page is rendered with its metadata and
 * content for search engines, then the React app (frontend/ports.html) takes over. Vessel pages and photos link to Open
 * Vessel Photos. The species, methodology and data pages are in [PortsContentController].
 *
 * "Pêche" alone mostly means angling on French search engines: the titles say "pêche professionnelle".
 */
@Controller
class PortsPagesController(
    private val htmlPageRenderer: HtmlPageRenderer,
    private val publicUrl: PublicUrl,
    private val getPorts: GetPorts,
    private val getPort: GetPort,
    private val getSpecies: GetSpecies,
) {
    @GetMapping(PORTS_PREFIX, "$PORTS_PREFIX/")
    fun map(): ResponseEntity<String> {
        val baseUrl = publicUrl.get(Site.PORTS)
        val ports = getPorts.execute()
        val species = getSpecies.execute()
        val updatedOn = getPorts.lastUpdate()
        val vesselCount = ports.sumOf { it.vesselCount }
        val auctions = ports.mapNotNull { it.auction }.sortedByDescending { it.valueThousandEuros }
        val auctionYear = auctions.maxOfOrNull { it.year }
        val description =
            "Chiffres de la pêche professionnelle en France : ${number(vesselCount)} navires de pêche dans " +
                "${ports.size} ports, leurs engins et leur taille, et les ventes de ${auctions.size} criées " +
                "(espèces, prix et saisons)."
        val portItems =
            ports.sortedByDescending { it.vesselCount }.joinToString("") { port ->
                """<li><a href="${PortsSeo.portPath(port.slug)}">${escapeHtml(port.port.name)}</a> : """ +
                    vessels(port.vesselCount) + (port.auction?.let { ", criée ${escapeHtml(it.auction)}" } ?: "") +
                    "</li>"
            }
        val slugByLocode = ports.associate { it.port.locode to it.slug }
        val auctionItems =
            auctions.joinToString("") {
                """<li><a href="${PortsSeo.portPath(slugByLocode.getValue(it.portLocode))}">""" +
                    "${escapeHtml(it.auction)}</a> : " +
                    "${number(it.quantityTonnes)} t vendues en ${it.year}, ${millions(it.valueThousandEuros)}</li>"
            }
        val speciesItems =
            species.take(TOP_SPECIES).joinToString("") {
                """<li><a href="${PortsSeo.speciesPath(it.slug)}">${escapeHtml(it.name)}</a> : """ +
                    "${number(it.quantityTonnes)} t, ${millions(it.valueThousandEuros)}" +
                    (it.priceEurosPerKg?.let { p -> ", ${price(p)}" } ?: "") + "</li>"
            }
        val questions = homeQuestions(ports, species, updatedOn?.year)

        return render(
            HtmlPage(
                title = "${Site.PORTS.siteName} — ${PortsSeo.TAGLINE}",
                description = description,
                canonicalPath = "/",
                imageUrl = PortsSeo.homeImage(baseUrl),
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "WebSite",
                            "name" to Site.PORTS.siteName,
                            "alternateName" to
                                listOf("La Pêche française — ${PortsSeo.TAGLINE}", "Ports de pêche de France"),
                            "url" to "$baseUrl/",
                            "description" to description,
                            "inLanguage" to "fr",
                        ),
                        PortsSeo.dataset(baseUrl, updatedOn, auctionYear),
                    ) + PortsSeo.faqStructuredData(questions),
                body =
                    "<main><h1>La pêche professionnelle en France, port par port</h1>" +
                        "<p>${escapeHtml(description)}</p>" +
                        "<p>Chaque navire est rattaché au port où il a été le plus souvent observé à quai. " +
                        "Pour chaque port : sa flotte par taille et par engin de pêche (chalut, filets, lignes, " +
                        "casiers, dragues…), ses navires et leurs photos, et ce que vend sa criée.</p>" +
                        PortsSeo.updatedOn(updatedOn) +
                        (PortsSeo.coverageSentence(getPorts.coverage())?.let { "<p>${escapeHtml(it)}</p>" } ?: "") +
                        PortsSeo.faqSection(questions) +
                        "<h2>Les ports de pêche français</h2><ul>$portItems</ul>" +
                        (
                            if (auctionItems.isEmpty()) {
                                ""
                            } else {
                                "<h2>Les criées : ventes $auctionYear</h2><ul>$auctionItems</ul>" +
                                    "<p>Source : ${PortsSeo.FRANCE_AGRIMER}, $auctionYear.</p>"
                            }
                        ) +
                        (
                            if (speciesItems.isEmpty()) {
                                ""
                            } else {
                                "<h2>Les espèces les plus vendues en criée</h2><ul>$speciesItems</ul>" +
                                    """<p><a href="${PortsSeo.SPECIES_PATH}">Toutes les espèces</a></p>"""
                            }
                        ) +
                        links() + "</main>",
            ),
        )
    }

    /** The old URL of a port page, by its UN/LOCODE (e.g. /FRQUY): moved to /ports/erquy */
    @GetMapping("$PORTS_PREFIX/{locode:[A-Za-z]{2}[A-Za-z0-9]{3}(?:-[0-9]{1,4})?}")
    fun portByLocode(
        @PathVariable locode: String,
    ): ResponseEntity<String> {
        val port = getPorts.execute().firstOrNull { it.port.locode == locode.uppercase() } ?: return notFound()

        return movedTo(PortsSeo.portPath(port.slug))
    }

    @GetMapping("$PORTS_PREFIX/ports", "$PORTS_PREFIX/ports/")
    fun ports(): ResponseEntity<String> = movedTo("/")

    @GetMapping("$PORTS_PREFIX/ports/{slug}")
    fun port(
        @PathVariable slug: String,
    ): ResponseEntity<String> {
        if (slug != slug.lowercase()) return movedTo(PortsSeo.portPath(slug.lowercase()))
        val portWithCount = getPorts.execute().firstOrNull { it.slug == slug } ?: return notFound()
        val details =
            try {
                getPort.execute(portWithCount.port.locode)
            } catch (_: BackendUsageException) {
                return notFound()
            }
        val updatedOn = getPorts.lastUpdate()
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
        val speciesSlugs = if (species.isEmpty()) emptyMap() else speciesSlugsByName(getSpecies.execute())
        val vesselCount = details.vessels.size
        val reliability = reliability(details.vessels.map { it.homePort })

        val title =
            if (auctionTotal != null) {
                "Port de pêche $ofPort : ${vessels(vesselCount)}, criée et chiffres ${auction.year}"
            } else {
                "Port de pêche $ofPort : ${vessels(vesselCount)} de pêche professionnelle"
            }
        val description =
            buildString {
                append("${vessels(vesselCount).replaceFirstChar { it.uppercase() }} de pêche professionnelle ")
                append("basé${if (vesselCount > 1) "s" else ""} au port $ofPort")
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
        val questions =
            buildList {
                add(
                    Question(
                        "Combien de bateaux de pêche au port $ofPort ?",
                        "${vessels(vesselCount).replaceFirstChar { it.uppercase() }} de pêche professionnelle " +
                            "${if (vesselCount > 1) "ont" else "a"} pour port d’attache le port $ofPort" +
                            (updatedOn?.let { " au ${PortsSeo.date(it)}" } ?: "") +
                            ", d’après leurs positions à quai et le ${PortsSeo.FLEET_REGISTER}.",
                    ),
                )
                gears.firstOrNull()?.let { (label, count) ->
                    add(
                        Question(
                            "Quels engins de pêche utilisent les navires du port $ofPort ?",
                            "Le premier engin des navires du port $ofPort est : ${label.lowercase()} " +
                                "(${vessels(count)}" +
                                gears.drop(1).take(2).joinToString("") { (l, c) -> ", puis ${l.lowercase()} ($c)" } +
                                "), d’après le ${PortsSeo.FLEET_REGISTER}.",
                        ),
                    )
                }
                if (auction != null && auctionTotal != null) {
                    val ofAuction = PageTexts.French.of(auction.name)
                    if (species.isNotEmpty()) {
                        add(
                            Question(
                                "Que vend la criée $ofAuction ?",
                                "En ${auction.year}, la criée $ofAuction a vendu " +
                                    "${number(auctionTotal.quantityTonnes)} t de produits de la mer pour " +
                                    "${millions(auctionTotal.valueThousandEuros)}, d’abord " +
                                    species.take(3).joinToString(", ") { it.species!!.lowercase() } +
                                    " (source : ${PortsSeo.FRANCE_AGRIMER}).",
                            ),
                        )
                    }
                    auctionTotal.priceEurosPerKg?.let {
                        add(
                            Question(
                                "Quel est le prix moyen du poisson à la criée $ofAuction ?",
                                "Le prix moyen des produits de la mer vendus à la criée $ofAuction était de " +
                                    "${price(it)} en ${auction.year}, toutes espèces confondues " +
                                    "(source : ${PortsSeo.FRANCE_AGRIMER}).",
                            ),
                        )
                    }
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
                append("<h2>${escapeHtml("Flotte de pêche du port $ofPort")}</h2>")
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
                append("<p>Source : ${PortsSeo.FLEET_REGISTER}, positions des navires.</p>")
                reliability?.let {
                    append("<h3>Fiabilité des données</h3><p>${escapeHtml(it)} ")
                    append("""<a href="${PortsSeo.METHODOLOGY_PATH}">Méthodologie</a></p>""")
                }
            }
        val auctionSection =
            if (auction == null || auctionTotal == null) {
                ""
            } else {
                "<h2>${escapeHtml(
                    "Criée ${PageTexts.French.of(auction.name)} : ventes et prix ${auction.year}",
                )}</h2>" +
                    "<p>${number(auctionTotal.quantityTonnes)} t de produits de la mer vendues en ${auction.year}, " +
                    "pour ${millions(auctionTotal.valueThousandEuros)}" +
                    (auctionTotal.priceEurosPerKg?.let { ", soit ${price(it)} en moyenne" } ?: "") +
                    ". Source : ${PortsSeo.FRANCE_AGRIMER}, ${auction.year}.</p>" +
                    "<h3>Principales espèces</h3><ul>" +
                    species.joinToString("") {
                        val name = escapeHtml(it.species!!.replaceFirstChar { c -> c.uppercase() })
                        val link =
                            speciesSlugs[ImportAuctionSales.normalize(it.species)]
                                ?.let { s -> """<a href="${PortsSeo.speciesPath(s)}">$name</a>""" }
                                ?: name

                        "<li>$link : ${number(it.quantityTonnes)} t" +
                            (it.priceEurosPerKg?.let { p -> ", ${price(p)}" } ?: "") + "</li>"
                    } + "</ul>"
            }
        val path = PortsSeo.portPath(slug)
        val portUrl = publicUrl.get(Site.PORTS) + path

        return render(
            HtmlPage(
                title = "$title | ${Site.PORTS.siteName}",
                description = description,
                canonicalPath = path,
                // A card with its figures: a random vessel photo would not say what the page is
                imageUrl = PortsSeo.portImage(publicUrl.get(Site.PORTS), slug),
                structuredData =
                    listOf(
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "Place",
                            "name" to "Port de pêche $ofPort",
                            "description" to description,
                            "url" to portUrl,
                            "identifier" to port.locode,
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
                        PortsSeo.breadcrumb(publicUrl.get(Site.PORTS), port.name to path),
                    ) + PortsSeo.faqStructuredData(questions),
                body =
                    "<main>" + PortsSeo.nav(port.name to null) +
                        "<h1>${escapeHtml("Port de pêche $ofPort")}</h1><p>${escapeHtml(description)}</p>" +
                        PortsSeo.updatedOn(updatedOn) +
                        fleet + auctionSection + PortsSeo.faqSection(questions) +
                        "<h2>Les navires</h2><ul>$items</ul>" + links() + "</main>",
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

    /** Each page with the day its data last changed, so that search engines fetch again only the changed ones */
    @GetMapping("$PORTS_PREFIX/sitemap.xml")
    @ResponseBody
    fun sitemap(): ResponseEntity<String> {
        val baseUrl = publicUrl.get(Site.PORTS)
        val lastModified = getPorts.lastUpdate()?.let { "<lastmod>$it</lastmod>" } ?: ""
        val paths =
            listOf(
                "/",
                PortsSeo.SPECIES_PATH,
                PortsSeo.SMALL_SCALE_PATH,
                PortsSeo.METHODOLOGY_PATH,
                PortsSeo.CHANGELOG_PATH,
            ) +
                getPorts.execute().map { PortsSeo.portPath(it.slug) } +
                getSpecies.execute().map { PortsSeo.speciesPath(it.slug) }
        val urls = paths.joinToString("") { "<url><loc>${escapeHtml(baseUrl + it)}</loc>$lastModified</url>" }

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

    companion object {
        private val FRENCH = Locale.FRANCE
        private const val TOP_SPECIES = 15

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

        fun vessels(count: Int) = if (count == 1) "1 navire" else "${number(count)} navires"

        /** The slug of the page of each species, by its normalized name (see [ImportAuctionSales.normalize]) */
        fun speciesSlugsByName(species: List<SpeciesSales>): Map<String, String> =
            species
                .flatMap { s ->
                    s.auctions.mapNotNull { it.species }.map { ImportAuctionSales.normalize(it) to s.slug }
                }.toMap()

        /** The links to the other pages, so that search engines find them all */
        fun links(): String =
            "<nav><h2>Aller plus loin</h2><ul>" +
                """<li><a href="${PortsSeo.SPECIES_PATH}">Les espèces vendues en criée : prix et saisons</a></li>""" +
                """<li><a href="${PortsSeo.SMALL_SCALE_PATH}">La petite pêche côtière en chiffres</a></li>""" +
                """<li><a href="${PortsSeo.METHODOLOGY_PATH}">Méthodologie et sources</a></li>""" +
                """<li><a href="${PortsSeo.CHANGELOG_PATH}">Historique des mises à jour</a></li>""" +
                """<li><a href="${PortsSeo.PORTS_CSV_PATH}">Télécharger les ports et leur flotte (CSV)</a></li>""" +
                """<li><a href="${PortsSeo.SALES_CSV_PATH}">Télécharger les ventes des criées (CSV)</a></li>""" +
                "</ul></nav>"

        /** The questions people ask about the French fishing industry, answered from the data in one sentence each */
        fun homeQuestions(
            ports: List<PortWithVesselCount>,
            species: List<SpeciesSales>,
            year: Int?,
        ): List<Question> {
            if (ports.isEmpty()) return emptyList()
            val vesselCount = ports.sumOf { it.vesselCount }
            val asOf = year?.let { " en $it" } ?: ""
            val auctions = ports.mapNotNull { port -> port.auction?.let { port to it } }
            val auctionYear = auctions.maxOfOrNull { it.second.year }
            val gears =
                GEAR_FAMILIES
                    .map { (label, codes) ->
                        label to
                            ports.sumOf { port ->
                                port.vesselCountByGear
                                    .filterKeys { it in codes }
                                    .values
                                    .sum()
                            }
                    }.filter { it.second > 0 }
                    .sortedByDescending { it.second }
            val biggestFleet = ports.maxBy { it.vesselCount }

            return buildList {
                add(
                    Question(
                        "Combien de bateaux de pêche en France ?",
                        "${number(vesselCount)} navires de pêche professionnelle français ont un port d’attache " +
                            "connu$asOf, répartis dans ${ports.size} ports (source : ${PortsSeo.FLEET_REGISTER} et " +
                            "positions des navires).",
                    ),
                )
                add(
                    Question(
                        "Quel est le premier port de pêche français ?",
                        "Par le nombre de navires, c’est le port ${PageTexts.French.of(biggestFleet.port.name)} " +
                            "(${vessels(biggestFleet.vesselCount)})" +
                            (
                                auctions.maxByOrNull { it.second.valueThousandEuros }?.let { (port, auction) ->
                                    " ; par la valeur vendue en criée, c’est le port " +
                                        "${PageTexts.French.of(port.port.name)} " +
                                        "(${millions(auction.valueThousandEuros)} en ${auction.year}, source : " +
                                        "${PortsSeo.FRANCE_AGRIMER})"
                                } ?: ""
                            ) + ".",
                    ),
                )
                if (auctions.isNotEmpty()) {
                    val tonnes = auctions.sumOf { it.second.quantityTonnes }
                    val value = auctions.sumOf { it.second.valueThousandEuros }
                    add(
                        Question(
                            "Combien de criées en France ?",
                            "${auctions.size} criées (halles à marée) ont vendu ${number(tonnes)} t de produits de " +
                                "la mer pour ${millions(value)} en $auctionYear (source : ${PortsSeo.FRANCE_AGRIMER}).",
                        ),
                    )
                }
                if (species.isNotEmpty()) {
                    add(
                        Question(
                            "Quelles sont les espèces les plus vendues en criée ?",
                            "En valeur, les espèces les plus vendues dans les criées françaises en " +
                                "${species.first().year} sont " +
                                species.take(5).joinToString(", ") {
                                    "${it.name.lowercase()} (${millions(it.valueThousandEuros)})"
                                } + " (source : ${PortsSeo.FRANCE_AGRIMER}).",
                        ),
                    )
                }
                if (gears.isNotEmpty()) {
                    add(
                        Question(
                            "Quels engins de pêche utilisent les bateaux français ?",
                            "Parmi les navires dont l’engin principal est connu : " +
                                gears.take(4).joinToString(", ") { (label, count) -> "${label.lowercase()} ($count)" } +
                                " (source : ${PortsSeo.FLEET_REGISTER}).",
                        ),
                    )
                }
            }
        }

        /**
         * How the home ports of a port's vessels were found, e.g. "12 navires y ont été vus à quai le plus souvent
         * (81 % de leurs positions au port en moyenne), 3 y ont fait le plus d’escales en un an (Global Fishing Watch)."
         */
        fun reliability(homePorts: List<HomePort>): String? {
            if (homePorts.isEmpty()) return null
            val observed = homePorts.filter { it.source == HomePortSource.IMPORT }
            val fromVisits = homePorts.filter { it.source == HomePortSource.GFW }
            val fromRegistration = homePorts.filter { it.source == HomePortSource.REGISTER }
            val corrected = homePorts.filter { it.source == HomePortSource.MANUAL }
            val shares =
                observed
                    .filter { it.observationsTotal > 0 }
                    .map { it.observationsAtPort.toDouble() / it.observationsTotal }
            val average = shares.takeIf { it.isNotEmpty() }?.average()
            val sharePart = average?.let { " (${PortsSeo.percent(it)} de leurs positions à quai en moyenne)" } ?: ""

            return listOfNotNull(
                observed.takeIf { it.isNotEmpty() }?.let {
                    val plural = it.size > 1
                    "${vessels(it.size)} ${if (plural) "y ont" else "y a"} été vu${if (plural) "s" else ""} à quai " +
                        "le plus souvent$sharePart"
                },
                fromVisits.takeIf { it.isNotEmpty() }?.let {
                    "${vessels(it.size)} ${if (it.size > 1) "y ont" else "y a"} fait le plus d’escales en un an " +
                        "(Global Fishing Watch)"
                },
                fromRegistration.takeIf { it.isNotEmpty() }?.let {
                    "${vessels(it.size)} n’${if (it.size > 1) "y sont rattachés" else "y est rattaché"} que par " +
                        "${if (it.size > 1) "leur" else "son"} quartier d’immatriculation"
                },
                corrected.takeIf { it.isNotEmpty() }?.let {
                    "${vessels(
                        it.size,
                    )} ${if (it.size > 1) "y ont" else "y a"} été rattaché${if (it.size > 1) "s" else ""} " +
                        "sur signalement d’un visiteur, vérifié"
                },
            ).joinToString(", ").replaceFirstChar { it.uppercase() } + "."
        }

        fun movedTo(path: String): ResponseEntity<String> =
            ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(path)).build()

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
