package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.entities.SpeciesSales
import org.openvesselphotos.domain.entities.slugOf
import org.openvesselphotos.domain.use_cases.public_api.GetPorts
import org.openvesselphotos.domain.use_cases.public_api.GetSpecies
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.GEAR_FAMILIES
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.links
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.millions
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.movedTo
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.number
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.price
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.vessels
import org.openvesselphotos.infrastructure.web.PortsSeo.Question
import org.openvesselphotos.infrastructure.web.SiteHostFilter.Companion.PORTS_PREFIX
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.time.Duration
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * The pages of the ports site beyond the map: the species sold in the auctions, the methodology and sources, the data
 * as CSV, and llms.txt. Each figure says its source and year: AI assistants cite such pages first. As the map pages
 * (see [PortsPagesController]), they are rendered for search engines, then shown as they are by the React app.
 */
@Controller
class PortsContentController(
    private val htmlPageRenderer: HtmlPageRenderer,
    private val publicUrl: PublicUrl,
    private val getPorts: GetPorts,
    private val getSpecies: GetSpecies,
    private val portsPagesController: PortsPagesController,
) {
    @GetMapping("$PORTS_PREFIX${PortsSeo.SPECIES_PATH}")
    fun speciesIndex(): ResponseEntity<String> {
        val species = getSpecies.execute()
        val year = species.firstOrNull()?.year
        val updatedOn = getPorts.lastUpdate()
        val description =
            if (year == null) {
                "Les espèces de poissons, coquillages et crustacés vendues dans les criées françaises."
            } else {
                "Les ${species.size} espèces vendues dans les criées françaises en $year : tonnages, valeur, prix " +
                    "moyen au kilo, premières criées et saisons de pêche (source : ${PortsSeo.FRANCE_AGRIMER})."
            }
        val questions =
            if (species.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    Question(
                        "Quelles espèces sont pêchées en France ?",
                        "En $year, ${species.size} espèces ont été vendues dans les criées françaises ; en valeur, " +
                            "d’abord " + species.take(5).joinToString(", ") { it.name.lowercase() } +
                            " (source : ${PortsSeo.FRANCE_AGRIMER}).",
                    ),
                    species.maxBy { it.quantityTonnes }.let {
                        Question(
                            "Quelle est l’espèce la plus débarquée en criée ?",
                            "En tonnage, c’est ${it.name.lowercase()} : ${number(it.quantityTonnes)} t vendues dans " +
                                "les criées françaises en $year (source : ${PortsSeo.FRANCE_AGRIMER}).",
                        )
                    },
                )
            }
        val rows =
            species.joinToString("") {
                """<tr><th scope="row"><a href="${PortsSeo.speciesPath(it.slug)}">${escapeHtml(it.name)}</a></th>""" +
                    "<td>${number(it.quantityTonnes)} t</td><td>${millions(it.valueThousandEuros)}</td>" +
                    "<td>${it.priceEurosPerKg?.let { p -> price(p) } ?: ""}</td></tr>"
            }

        return render(
            HtmlPage(
                title =
                    "Espèces pêchées en France : prix du poisson en criée" + (year?.let { " $it" } ?: "") +
                        " | ${Site.PORTS.siteName}",
                description = description,
                canonicalPath = PortsSeo.SPECIES_PATH,
                imageUrl = PortsSeo.homeImage(baseUrl()),
                structuredData =
                    listOf(PortsSeo.breadcrumb(baseUrl(), "Espèces" to PortsSeo.SPECIES_PATH)) +
                        PortsSeo.faqStructuredData(questions),
                body =
                    "<main>" + PortsSeo.nav("Espèces" to null) +
                        "<h1>Les espèces vendues en criée et leur prix</h1><p>${escapeHtml(description)}</p>" +
                        PortsSeo.updatedOn(updatedOn) +
                        PortsSeo.faqSection(questions) +
                        (
                            if (rows.isEmpty()) {
                                ""
                            } else {
                                "<h2>Les espèces en $year, par valeur vendue</h2>" +
                                    "<table><thead><tr><th scope=\"col\">Espèce</th><th scope=\"col\">Quantité</th>" +
                                    "<th scope=\"col\">Valeur</th><th scope=\"col\">Prix moyen</th></tr></thead>" +
                                    "<tbody>$rows</tbody></table>" +
                                    "<p>Source : ${PortsSeo.FRANCE_AGRIMER}, $year.</p>"
                            }
                        ) + links() + "</main>",
            ),
        )
    }

    @GetMapping("$PORTS_PREFIX${PortsSeo.SPECIES_PATH}/{slug}")
    fun species(
        @PathVariable slug: String,
    ): ResponseEntity<String> {
        if (slug != slug.lowercase()) return movedTo(PortsSeo.speciesPath(slug.lowercase()))
        val species = getSpecies.execute().firstOrNull { it.slug == slug } ?: return portsPagesController.notFound()
        val ports = getPorts.execute()
        val portsByLocode = ports.associateBy { it.port.locode }
        val year = species.year
        val name = species.name
        val ofSpecies = ofSpecies(name)
        val updatedOn = getPorts.lastUpdate()
        val trend = trend(species)
        val months = seasonMonths(species)
        val topAuctions = species.auctions.take(TOP_AUCTIONS)
        val description =
            "$name : ${number(species.quantityTonnes)} t vendues dans les criées françaises en $year pour " +
                millions(species.valueThousandEuros) +
                (species.priceEurosPerKg?.let { ", soit ${price(it)} en moyenne" } ?: "") +
                (topAuctions.firstOrNull()?.let { ". Première criée : ${it.auction}" } ?: "") + "."
        val questions =
            buildList {
                species.priceEurosPerKg?.let {
                    add(
                        Question(
                            "Quel est le prix du kilo $ofSpecies en criée ?",
                            "Dans les criées françaises, le kilo $ofSpecies s’est vendu ${price(it)} en moyenne en " +
                                "$year, pour ${number(
                                    species.quantityTonnes,
                                )} t (source : ${PortsSeo.FRANCE_AGRIMER}).",
                        ),
                    )
                }
                if (topAuctions.isNotEmpty()) {
                    add(
                        Question(
                            "Dans quelles criées se vend le plus $ofSpecies ?",
                            "En $year : " +
                                topAuctions.take(3).joinToString(", ") {
                                    "${it.auction} (${number(it.quantityTonnes)} t)"
                                } + " (source : ${PortsSeo.FRANCE_AGRIMER}).",
                        ),
                    )
                }
                if (months.isNotEmpty()) {
                    add(
                        Question(
                            "Quand se vend le plus $ofSpecies ?",
                            "Les criées françaises en vendent le plus en " + months.joinToString(", ") +
                                " (ventes mensuelles $year, source : ${PortsSeo.FRANCE_AGRIMER}).",
                        ),
                    )
                }
            }
        val auctionRows =
            topAuctions.joinToString("") { sale ->
                val port = sale.portLocode?.let { portsByLocode[it] }
                val auction = escapeHtml(sale.auction)

                "<tr><th scope=\"row\">" +
                    (port?.let { """<a href="${PortsSeo.portPath(it.slug)}">$auction</a>""" } ?: auction) +
                    "</th><td>${number(sale.quantityTonnes)} t</td><td>${millions(sale.valueThousandEuros)}</td>" +
                    "<td>${sale.priceEurosPerKg?.let { price(it) } ?: ""}</td></tr>"
            }
        val monthlyItems =
            species.monthlySales.joinToString("") {
                "<li>${monthName(it.month)} : ${number(it.quantityTonnes)} t</li>"
            }
        val path = PortsSeo.speciesPath(slug)

        return render(
            HtmlPage(
                title = "$name : prix en criée, ports et saisons $year | ${Site.PORTS.siteName}",
                description = description,
                canonicalPath = path,
                imageUrl = PortsSeo.speciesImage(baseUrl(), slug),
                structuredData =
                    listOf(
                        PortsSeo.breadcrumb(baseUrl(), "Espèces" to PortsSeo.SPECIES_PATH, name to path),
                    ) + PortsSeo.faqStructuredData(questions),
                body =
                    "<main>" + PortsSeo.nav("Espèces" to PortsSeo.SPECIES_PATH, name to null) +
                        "<h1>${escapeHtml(name)} : prix en criée, ports et saisons</h1>" +
                        "<p>${escapeHtml(description)}</p>" + (trend?.let { "<p>${escapeHtml(it)}</p>" } ?: "") +
                        PortsSeo.updatedOn(updatedOn) +
                        PortsSeo.faqSection(questions) +
                        (
                            if (auctionRows.isEmpty()) {
                                ""
                            } else {
                                "<h2>${escapeHtml("Les criées qui vendent le plus $ofSpecies")} ($year)</h2>" +
                                    "<table><thead><tr><th scope=\"col\">Criée</th><th scope=\"col\">Quantité</th>" +
                                    "<th scope=\"col\">Valeur</th><th scope=\"col\">Prix moyen</th></tr></thead>" +
                                    "<tbody>$auctionRows</tbody></table>"
                            }
                        ) +
                        (
                            if (monthlyItems.isEmpty()) {
                                ""
                            } else {
                                "<h2>${escapeHtml("Ventes mois par mois en $year")}</h2><ul>$monthlyItems</ul>"
                            }
                        ) +
                        "<p>Source : ${PortsSeo.FRANCE_AGRIMER}, $year.</p>" + links() + "</main>",
            ),
        )
    }

    @GetMapping("$PORTS_PREFIX${PortsSeo.METHODOLOGY_PATH}")
    fun methodology(): ResponseEntity<String> {
        val updatedOn = getPorts.lastUpdate()
        val auctionYear = getPorts.execute().mapNotNull { it.auction?.year }.maxOrNull()
        val gearItems =
            GEAR_FAMILIES.joinToString(
                "",
            ) { (label, codes) -> "<li>$label : ${codes.sorted().joinToString(", ")}</li>" }
        val description =
            "Comment ${Site.PORTS.siteName} compte les navires de pêche professionnelle de chaque port et les ventes " +
                "des criées : sources (FranceAgriMer, registre de la flotte de l’UE, Global Fishing Watch), méthode, " +
                "limites et données à télécharger."

        return render(
            HtmlPage(
                title = "Méthodologie et sources | ${Site.PORTS.siteName}",
                description = description,
                canonicalPath = PortsSeo.METHODOLOGY_PATH,
                imageUrl = PortsSeo.homeImage(baseUrl()),
                structuredData =
                    listOf(
                        PortsSeo.breadcrumb(baseUrl(), "Méthodologie" to PortsSeo.METHODOLOGY_PATH),
                        PortsSeo.dataset(baseUrl(), updatedOn, auctionYear),
                    ),
                body =
                    "<main>" + PortsSeo.nav("Méthodologie" to null) +
                        "<h1>Méthodologie et sources</h1><p>${escapeHtml(description)}</p>" +
                        PortsSeo.updatedOn(updatedOn) +
                        "<h2>Le port d’attache des navires</h2>" +
                        (PortsSeo.coverageSentence(getPorts.coverage())?.let { "<p>${escapeHtml(it)}</p>" } ?: "") +
                        "<p>Chaque navire de pêche professionnelle est rattaché au port où il a été vu le plus " +
                        "souvent, d’après ses retours au port relevés. Sans relevé, c’est le port où il a été le " +
                        "plus souvent observé à quai, d’après ses positions (AIS). Pour les navires du " +
                        """<a href="${PortsSeo.FLEET_REGISTER_URL}">${PortsSeo.FLEET_REGISTER}</a> """ +
                        "sans observation, c’est le port qu’il a le plus visité en un an d’après " +
                        """<a href="${PortsSeo.GFW_URL}">Global Fishing Watch</a>. """ +
                        "Le port d’attache ainsi trouvé peut différer du quartier d’immatriculation, " +
                        "inscrit sur la coque.</p>" +
                        "<p>Les petits navires n’ont souvent pas d’AIS : ni observés à quai, ni suivis par Global " +
                        "Fishing Watch. Faute de mieux, ils sont rattachés au port de leur quartier " +
                        "d’immatriculation (ou quartier maritime), inscrit au registre et sur la coque (GV pour " +
                        "Le Guilvinec, CC pour " +
                        "Concarneau…) : le port du même nom, ou à défaut celui où sont basés la plupart des navires " +
                        "du quartier dont le port d’attache est connu. C’est une approximation : un quartier compte " +
                        "souvent plusieurs ports (celui du Guilvinec couvre aussi Saint-Guénolé, Lesconil ou " +
                        "Loctudy). Ces navires sont signalés comme tels dans la liste de chaque port, et en changent " +
                        "dès qu’une meilleure source donne le leur.</p>" +
                        "<h2>Les engins de pêche et la taille des navires</h2>" +
                        "<p>L’engin principal et la longueur de chaque navire viennent du " +
                        "${PortsSeo.FLEET_REGISTER}. Les engins sont regroupés en familles, par leurs codes FAO " +
                        "(ISSCFG) :</p><ul>$gearItems</ul>" +
                        "<p>Tailles : petite pêche (moins de 12 m), pêche au large (12 à 24 m), pêche industrielle " +
                        "(24 m et plus).</p>" +
                        "<h2>Les ventes des criées</h2>" +
                        "<p>Les tonnages et valeurs par criée et par espèce sont ceux des « ventes déclarées en " +
                        """halles à marée » publiées chaque année par <a href="${PortsSeo.FRANCE_AGRIMER_URL}">""" +
                        "FranceAgriMer</a>" + (auctionYear?.let { ", ici pour $it" } ?: "") +
                        ". Le prix moyen est la valeur divisée par la quantité. Seules les ventes en criée sont " +
                        "comptées : pas la vente directe, ni le poisson débarqué à l’étranger.</p>" +
                        "<h2>Les photos des navires</h2>" +
                        """<p>Elles viennent d’<a href="${publicUrl.get()}/fr">Open Vessel Photos</a>, """ +
                        "chacune avec son auteur et sa licence.</p>" +
                        "<h2>Mises à jour</h2>" +
                        "<p>Les ports d’attache sont mis à jour à chaque nouvel import des positions, les ventes des " +
                        "criées à chaque publication annuelle de FranceAgriMer. Chaque mise à jour est datée dans " +
                        """l’<a href="${PortsSeo.CHANGELOG_PATH}">historique des mises à jour</a>.</p>""" +
                        "<h2>Télécharger les données</h2><ul>" +
                        """<li><a href="${PortsSeo.PORTS_CSV_PATH}">Les ports et leur flotte</a> (CSV)</li>""" +
                        """<li><a href="${PortsSeo.SALES_CSV_PATH}">Les ventes des criées par espèce</a> (CSV)</li>""" +
                        """<li><a href="/llms.txt">llms.txt</a>, pour les assistants d’IA</li></ul>""" +
                        links() + "</main>",
            ),
        )
    }

    /** The updates of the data, by day, the latest first */
    @GetMapping("$PORTS_PREFIX${PortsSeo.CHANGELOG_PATH}")
    fun changelog(): ResponseEntity<String> {
        val updates = getPorts.updates()
        val coverage = PortsSeo.coverageSentence(getPorts.coverage())
        val description =
            "Les mises à jour des données de ${Site.PORTS.siteName} : ports d’attache des navires, registre de la " +
                "flotte, ventes des criées, armateurs."
        val items =
            updates.groupBy { it.updatedOn }.entries.joinToString("") { (day, ofDay) ->
                """<h2><time datetime="$day">${PortsSeo.date(day)}</time></h2><ul>""" +
                    ofDay.joinToString("") {
                        "<li><strong>${escapeHtml(it.source.label)}</strong> : ${escapeHtml(it.summary)}</li>"
                    } + "</ul>"
            }

        return render(
            HtmlPage(
                title = "Historique des mises à jour | ${Site.PORTS.siteName}",
                description = description,
                canonicalPath = PortsSeo.CHANGELOG_PATH,
                imageUrl = PortsSeo.homeImage(baseUrl()),
                structuredData = listOf(PortsSeo.breadcrumb(baseUrl(), "Mises à jour" to PortsSeo.CHANGELOG_PATH)),
                body =
                    "<main>" + PortsSeo.nav("Mises à jour" to null) +
                        "<h1>Historique des mises à jour</h1><p>${escapeHtml(description)}</p>" +
                        (coverage?.let { "<p>${escapeHtml(it)}</p>" } ?: "") +
                        items.ifEmpty { "<p>Aucune mise à jour pour l’instant.</p>" } + links() + "</main>",
            ),
        )
    }

    /** Each port with its fleet by family of gears and its auction's sales in its last year */
    @GetMapping("$PORTS_PREFIX${PortsSeo.PORTS_CSV_PATH}")
    fun portsCsv(): ResponseEntity<String> {
        val header =
            listOf("locode", "slug", "name", "country", "latitude", "longitude", "vessels") +
                GEAR_FAMILIES.map { "vessels_${slugOfLabel(it.first)}" } +
                listOf("auction", "auction_year", "auction_quantity_t", "auction_value_keur")
        val rows =
            getPorts.execute().map { port ->
                listOf(
                    port.port.locode,
                    port.slug,
                    port.port.name,
                    port.port.country,
                    port.port.latitude,
                    port.port.longitude,
                    port.vesselCount,
                ) +
                    GEAR_FAMILIES.map { (_, codes) ->
                        port.vesselCountByGear
                            .filterKeys { it in codes }
                            .values
                            .sum()
                    } +
                    listOf(
                        port.auction?.auction,
                        port.auction?.year,
                        port.auction?.quantityTonnes,
                        port.auction?.valueThousandEuros,
                    )
            }

        return csv("ports-de-peche.csv", header, rows)
    }

    /** The sales of each species at each auction, all years (FranceAgriMer) */
    @GetMapping("$PORTS_PREFIX${PortsSeo.SALES_CSV_PATH}")
    fun salesCsv(): ResponseEntity<String> {
        val slugByLocode = getPorts.execute().associate { it.port.locode to it.slug }
        val rows =
            getSpecies.execute().flatMap { species ->
                species.auctions.map {
                    listOf(
                        it.auction,
                        it.portLocode,
                        it.portLocode?.let { locode -> slugByLocode[locode] },
                        it.year,
                        species.name,
                        species.slug,
                        it.quantityTonnes,
                        it.valueThousandEuros,
                        it.priceEurosPerKg?.let { p -> Math.round(p * 100) / 100.0 },
                    )
                }
            }

        return csv(
            "ventes-criees.csv",
            listOf(
                "auction",
                "port_locode",
                "port_slug",
                "year",
                "species",
                "species_slug",
                "quantity_t",
                "value_keur",
                "price_eur_kg",
            ),
            rows,
        )
    }

    /** What the site is and where its figures are, for AI assistants (llmstxt.org); see [CrawlersController.llms] */
    @GetMapping("$PORTS_PREFIX/llms.txt", produces = ["text/markdown;charset=UTF-8"])
    fun llms(): ResponseEntity<String> {
        val baseUrl = baseUrl()
        val ports = getPorts.execute()
        val species = getSpecies.execute()
        val updatedOn = getPorts.lastUpdate()
        val auctions = ports.mapNotNull { it.auction }
        val figures =
            PortsPagesController
                .homeQuestions(ports, species, updatedOn?.year)
                .joinToString("\n") { "- ${it.question} ${it.answer}" }
        val auctionYear = auctions.maxOfOrNull { it.year }?.let { " (ventes $it)" } ?: ""
        val updated = updatedOn?.let { " Mis à jour le ${PortsSeo.date(it)}." } ?: ""
        val examplePort =
            ports.maxByOrNull { it.vesselCount }?.let {
                ", ex. [${it.port.name}]($baseUrl${PortsSeo.portPath(
                    it.slug,
                )})"
            }
        val exampleSpecies =
            species.firstOrNull()?.let { ", ex. [${it.name}]($baseUrl${PortsSeo.speciesPath(it.slug)})" }
        val body =
            """
            |# ${Site.PORTS.siteName}
            |
            |> ${Site.PORTS.siteName} : ${PortsSeo.TAGLINE} en France. Les navires de pêche professionnelle de
            |> chaque port, leurs engins et leur taille, et les ventes des criées par espèce, avec leurs sources.
            |
            |En français. Pêche maritime professionnelle uniquement (pas la pêche de loisir). Chaque chiffre est daté et
            |sourcé : ${PortsSeo.FRANCE_AGRIMER}$auctionYear, ${PortsSeo.FLEET_REGISTER}, Global Fishing Watch.$updated
            |
            |## Chiffres clés
            |
            |$figures
            |
            |## Pages
            |
            |- [Carte des ports de pêche]($baseUrl/): ${ports.size} ports, ${vessels(ports.sumOf { it.vesselCount })}
            |- Un port, sa flotte par engin et par taille, ses navires et sa criée : `$baseUrl/ports/{slug}`${examplePort ?: ""}
            |- [Espèces vendues en criée]($baseUrl${PortsSeo.SPECIES_PATH}): prix, tonnages et premières criées
            |- Une espèce : `$baseUrl${PortsSeo.SPECIES_PATH}/{slug}`${exampleSpecies ?: ""}
            |- [La petite pêche côtière en chiffres]($baseUrl${PortsSeo.SMALL_SCALE_PATH}): emplois, valeur ajoutée et limites, d’après la recherche française de 2025
            |- [Méthodologie et sources]($baseUrl${PortsSeo.METHODOLOGY_PATH})
            |- [Historique des mises à jour]($baseUrl${PortsSeo.CHANGELOG_PATH})
            |
            |## Données
            |
            |- [Les ports et leur flotte, CSV]($baseUrl${PortsSeo.PORTS_CSV_PATH})
            |- [Les ventes des criées par espèce, CSV]($baseUrl${PortsSeo.SALES_CSV_PATH})
            |- API JSON : `$baseUrl/v1/ports` (les ports) et `$baseUrl/v1/ports/{LOCODE}` (un port et ses navires)
            |- [Sitemap]($baseUrl/sitemap.xml)
            |
            """.trimMargin()

        return ResponseEntity
            .ok()
            .contentType(MediaType("text", "markdown", Charsets.UTF_8))
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(body)
    }

    private fun baseUrl() = publicUrl.get(Site.PORTS)

    private fun render(page: HtmlPage) = htmlPageRenderer.render(page.copy(language = Language.FR, site = Site.PORTS))

    companion object {
        private const val TOP_AUCTIONS = 10
        private val FRENCH = Locale.FRANCE

        /** "de sole", "d’anchois": the species is named as in FranceAgriMer's workbook, without article */
        fun ofSpecies(name: String): String {
            val lowercase = name.lowercase()

            return if (lowercase.firstOrNull()?.let { it in "aeiouyhàâäéèêëîïôöùûü" } == true) {
                "d’$lowercase"
            } else {
                "de $lowercase"
            }
        }

        fun monthName(month: Int): String = Month.of(month).getDisplayName(TextStyle.FULL, FRENCH)

        /** The months it sold the most in, in their order: those above the monthly average, at most 4 */
        fun seasonMonths(species: SpeciesSales): List<String> {
            val monthly = species.monthlySales.filter { it.quantityTonnes > 0 }
            if (monthly.size < 2) return emptyList()
            val average = monthly.sumOf { it.quantityTonnes } / 12

            return monthly
                .filter { it.quantityTonnes > average }
                .sortedByDescending { it.quantityTonnes }
                .take(4)
                .sortedBy { it.month }
                .map { monthName(it.month) }
        }

        /** e.g. "En hausse de 12 % en quantité par rapport à 2024." */
        fun trend(species: SpeciesSales): String? {
            val previous = species.previousQuantityTonnes?.takeIf { it > 0 } ?: return null
            val change = Math.round((species.quantityTonnes - previous) / previous * 100)

            return when {
                change > 0 -> "En hausse de $change % en quantité par rapport à ${species.year - 1}."
                change < 0 -> "En baisse de ${-change} % en quantité par rapport à ${species.year - 1}."
                else -> "Stable en quantité par rapport à ${species.year - 1}."
            }
        }

        private fun slugOfLabel(label: String) = slugOf(label).replace('-', '_')

        fun csvCell(value: Any?): String {
            val text = value?.toString() ?: ""

            return if (text.any { it in ",\"\n\r" }) "\"${text.replace("\"", "\"\"")}\"" else text
        }

        fun csv(
            fileName: String,
            header: List<String>,
            rows: List<List<Any?>>,
        ): ResponseEntity<String> =
            ResponseEntity
                .ok()
                .contentType(MediaType("text", "csv", Charsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$fileName\"")
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body((listOf(header) + rows).joinToString("") { row -> row.joinToString(",") { csvCell(it) } + "\n" })
    }
}
