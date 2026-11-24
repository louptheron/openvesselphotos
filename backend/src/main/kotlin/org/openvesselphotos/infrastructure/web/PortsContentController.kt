package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.entities.SpeciesSales
import org.openvesselphotos.domain.entities.SpeciesStocks
import org.openvesselphotos.domain.entities.StockStatus
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
        val stockAssessment = getSpecies.stockAssessment()
        val hasStocks = species.any { it.stocks != null }
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
                ) +
                    species
                        .filter { it.stocks?.status == StockStatus.BON_ETAT }
                        .takeIf { it.isNotEmpty() }
                        ?.let { sustainable ->
                            Question(
                                "Quelles espèces sont pêchées durablement ?",
                                "D’après l’Ifremer, les espèces vendues en criée dont les stocks sont en bon état " +
                                    "sont, d’abord en valeur : " +
                                    sustainable.take(8).joinToString(", ") { it.name.lowercase() } +
                                    " (source : ${PortsSeo.IFREMER}, ${stockAssessment.edition}).",
                            )
                        }.let { listOfNotNull(it) }
            }
        val rows =
            species.joinToString("") {
                """<tr><th scope="row"><a href="${PortsSeo.speciesPath(it.slug)}">${escapeHtml(it.name)}</a></th>""" +
                    "<td>${number(it.quantityTonnes)} t</td><td>${millions(it.valueThousandEuros)}</td>" +
                    "<td>${it.priceEurosPerKg?.let { p -> price(p) } ?: ""}</td>" +
                    (if (hasStocks) """<td class="stock">${stockCell(it.stocks)}</td>""" else "") + "</tr>"
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
                                    "<th scope=\"col\">Valeur</th><th scope=\"col\">Prix moyen</th>" +
                                    (if (hasStocks) "<th scope=\"col\" class=\"stock\">État des stocks</th>" else "") +
                                    "</tr></thead><tbody>$rows</tbody></table>" +
                                    "<p>Sources : ${PortsSeo.FRANCE_AGRIMER}, $year" +
                                    (
                                        if (hasStocks) {
                                            """ ; <a href="${escapeHtml(stockAssessment.url)}">""" +
                                                "${PortsSeo.IFREMER}</a>, ${stockAssessment.edition}. " +
                                                "Un stock est une population d’une espèce dans une zone de pêche ; " +
                                                """<a href="${PortsSeo.METHODOLOGY_PATH}#stocks">ce que veut dire """ +
                                                "chaque état</a>"
                                        } else {
                                            ""
                                        }
                                    ) + ".</p>"
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
        val stockAssessment = getSpecies.stockAssessment()
        val gearItems =
            GEAR_FAMILIES.joinToString(
                "",
            ) { (label, codes) -> "<li>$label : ${codes.sorted().joinToString(", ")}</li>" }
        val description =
            "D’où viennent les chiffres de ${Site.PORTS.siteName}, comment chaque navire est rattaché à un port et " +
                "ce que les données ne disent pas. Les sources sont FranceAgriMer, le registre de la flotte de l’UE, " +
                "Global Fishing Watch et l’Ifremer."

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
                        "<p>Nous rattachons chaque navire au port où il revient le plus souvent, d’après ses " +
                        "retours au port relevés par nos bénévoles. Quand il n’y en a pas, nous prenons le port " +
                        "où ses positions AIS le montrent le plus souvent à quai. Pour les navires du " +
                        """<a href="${PortsSeo.FLEET_REGISTER_URL}">${PortsSeo.FLEET_REGISTER}</a> """ +
                        "qui n’ont jamais été vus à quai, c’est le port qu’ils ont le plus fréquenté sur un an selon " +
                        """<a href="${PortsSeo.GFW_URL}">Global Fishing Watch</a>. """ +
                        "Ce port n’est pas forcément celui du quartier d’immatriculation peint sur la coque.</p>" +
                        "<p>Beaucoup de petits navires n’ont pas d’AIS. On ne les voit donc ni à quai, ni dans " +
                        "Global Fishing Watch. Ceux-là sont rattachés à leur quartier maritime, celui dont les " +
                        "lettres figurent au registre et sur la coque (GV pour Le Guilvinec, CC pour Concarneau, " +
                        "etc.). Nous prenons le port qui porte le nom du quartier ou, s’il n’y en a pas, celui où " +
                        "sont basés la plupart des autres navires du quartier. C’est approximatif, car un quartier " +
                        "regroupe souvent plusieurs ports : celui du Guilvinec couvre aussi Saint-Guénolé, Lesconil " +
                        "et Loctudy. Ces navires sont signalés dans la liste de chaque port, et nous les déplaçons " +
                        "dès qu’une meilleure source indique leur vrai port.</p>" +
                        "<h2>Les engins de pêche et la taille des navires</h2>" +
                        "<p>L’engin principal et la longueur de chaque navire sont ceux déclarés au " +
                        "${PortsSeo.FLEET_REGISTER}. Nous regroupons les engins en grandes familles à partir de " +
                        "leurs codes FAO (ISSCFG) :</p><ul>$gearItems</ul>" +
                        "<p>Pour la taille, nous distinguons la petite pêche (moins de 12 m), la pêche au large " +
                        "(de 12 à 24 m) et la pêche industrielle (24 m et plus).</p>" +
                        "<h2>Les ventes des criées</h2>" +
                        "<p>Les tonnages et les montants par criée et par espèce viennent des « ventes déclarées " +
                        "en halles à marée », que " +
                        """<a href="${PortsSeo.FRANCE_AGRIMER_URL}">FranceAgriMer</a> publie chaque année""" +
                        (auctionYear?.let { ". Les chiffres affichés sont ceux de $it" } ?: "") +
                        ". Le prix moyen est simplement la valeur divisée par la quantité. Seules les ventes en " +
                        "criée y figurent, sans la vente directe ni le poisson débarqué à l’étranger.</p>" +
                        """<h2 id="stocks">L’état des stocks</h2>""" +
                        "<p>Un stock est la population d’une espèce dans une zone de pêche : la sole de la Manche " +
                        "Est n’est pas celle du golfe de Gascogne. L’état de chaque stock vient du " +
                        """<a href="${escapeHtml(stockAssessment.url)}">bilan ${stockAssessment.edition}</a> de """ +
                        "l’Ifremer, qui classe les stocks débarqués par la pêche française d’après la pression de " +
                        "pêche et la quantité de poissons en âge de se reproduire :</p><ul>" +
                        "<li>en bon état : pêché sans excès, avec assez de reproducteurs ;</li>" +
                        "<li>reconstituable : pêché sans excès, mais avec encore trop peu de reproducteurs ;</li>" +
                        "<li>surpêché : pêché plus qu’il ne peut durablement le supporter ;</li>" +
                        "<li>surpêché et dégradé : surpêché, et avec trop peu de reproducteurs ;</li>" +
                        "<li>effondré : trop peu de reproducteurs pour que le stock se renouvelle ;</li>" +
                        "<li>non classifié : évalué, mais sans seuils de référence.</li></ul>" +
                        "<p>Une espèce pêchée dans plusieurs stocks dans des états différents est dite « variable " +
                        "selon le stock ». Sur la carte, chaque port montre l’état des stocks de sa façade " +
                        "(Manche Est et mer du Nord, Manche Ouest et mer Celtique, golfe de Gascogne, Méditerranée). " +
                        "Beaucoup d’espèces ne sont pas évaluées (maigre, dorade royale, congre, saint-pierre, " +
                        "etc.) : elles n’ont pas d’état affiché.</p>" +
                        "<h2>Les photos des navires</h2>" +
                        """<p>Elles viennent d’<a href="${publicUrl.get()}/fr">Open Vessel Photos</a>. """ +
                        "Chaque photo est créditée à son auteur et publiée sous sa licence.</p>" +
                        "<h2>Mises à jour</h2>" +
                        "<p>Les ports d’attache sont recalculés à chaque nouvel import de positions. Les ventes des " +
                        "criées sont mises à jour quand FranceAgriMer publie ses chiffres de l’année. Toutes les " +
                        """mises à jour sont datées dans l’<a href="${PortsSeo.CHANGELOG_PATH}">historique</a>.</p>""" +
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

    /** The legal notice, mandatory for any French website (loi pour la confiance dans l’économie numérique, art. 6) */
    @GetMapping("$PORTS_PREFIX${PortsSeo.LEGAL_PATH}")
    fun legalNotice(): ResponseEntity<String> {
        val siteName = Site.PORTS.siteName
        val contact = """<a href="mailto:${LegalNotice.CONTACT_EMAIL}">${LegalNotice.CONTACT_EMAIL}</a>"""

        return render(
            HtmlPage(
                title = "Mentions légales | $siteName",
                description = "L’éditeur, l’hébergeur et les données personnelles de $siteName.",
                canonicalPath = PortsSeo.LEGAL_PATH,
                structuredData = listOf(PortsSeo.breadcrumb(baseUrl(), "Mentions légales" to PortsSeo.LEGAL_PATH)),
                body =
                    "<main>" + PortsSeo.nav("Mentions légales" to null) + "<h1>Mentions légales</h1>" +
                        "<h2>Éditeur</h2><p>$siteName est édité à titre personnel par ${LegalNotice.PUBLISHER}. " +
                        "Contact : $contact.</p>" +
                        "<h2>Directeur de la publication</h2><p>${LegalNotice.PUBLISHER}.</p>" +
                        "<h2>Hébergeur</h2><p>${LegalNotice.HOST_FR}</p>" +
                        "<h2>Données et contenus</h2><p>Les chiffres viennent de sources publiques, citées avec leur " +
                        """année dans la <a href="${PortsSeo.METHODOLOGY_PATH}">méthodologie</a>. Les photos des """ +
                        """navires viennent d’<a href="${LegalNotice.OPEN_VESSEL_PHOTOS_URL}/fr">Open Vessel """ +
                        "Photos</a>, chacune sous sa licence libre et avec le nom de son auteur. Fond de carte © les " +
                        """contributeurs d’<a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>. """ +
                        """Le <a href="${LegalNotice.SOURCE_CODE_URL}">code du site</a> est libre, sous licence """ +
                        "AGPL-3.0.</p>" +
                        "<h2>Données personnelles</h2><p>Le site ne dépose aucun cookie et ne mesure pas l’audience " +
                        "de ses visiteurs : il compte seulement les pages vues, sans adresse IP ni identifiant. Un " +
                        "port d’attache proposé est conservé avec le nom que son auteur choisit d’indiquer, le cas " +
                        "échéant. Pour exercer vos droits d’accès, de rectification et d’effacement (RGPD), " +
                        "écrivez à $contact.</p>" + links() + "</main>",
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

    /**
     * The status of the stocks of a species, as a pill styled by the React app (see ContentPage), with each stock in
     * its title
     */
    private fun stockCell(stocks: SpeciesStocks?): String {
        if (stocks == null) return ""
        val status = stocks.status
        val title =
            (status?.let { "Stock ${it.label} (Ifremer) : " } ?: "Stocks dans des états différents (Ifremer) : ") +
                stocks.stocks.joinToString(" ; ") { "${it.region.label}, ${number(it.tonnes)} t : ${it.status.label}" }

        return """<span class="stock-status" data-stock="${status?.name ?: "VARIABLE"}" """ +
            """title="${escapeHtml(title)}">${STOCK_LABELS[status]}</span>"""
    }

    private fun render(page: HtmlPage) = htmlPageRenderer.render(page.copy(language = Language.FR, site = Site.PORTS))

    companion object {
        /** As in the port sheet of the React app (see AuctionSales); null: variable */
        private val STOCK_LABELS =
            mapOf(
                StockStatus.BON_ETAT to "Bon état",
                StockStatus.RECONSTITUABLE to "Reconstituable",
                StockStatus.SURPECHE to "Surpêché",
                StockStatus.SURPECHE_DEGRADE to "Surpêché, dégradé",
                StockStatus.EFFONDRE to "Effondré",
                StockStatus.NON_CLASSIFIE to "Non classifié",
                null to "Variable",
            )

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
