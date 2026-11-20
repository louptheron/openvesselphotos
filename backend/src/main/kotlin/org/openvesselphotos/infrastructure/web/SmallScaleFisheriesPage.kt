package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.use_cases.public_api.GetPorts
import org.openvesselphotos.infrastructure.web.PortsPagesController.Companion.links
import org.openvesselphotos.infrastructure.web.PortsSeo.Question
import org.openvesselphotos.infrastructure.web.SiteHostFilter.Companion.PORTS_PREFIX
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * The small-scale coastal fisheries in figures, from French academic work published in 2025: what they bring to
 * jobs and coastal territories, and their limits. Every figure is quoted from its source, with its definition and its
 * years: the two main sources do not define the small-scale fleet the same way.
 */
@Controller
class SmallScaleFisheriesPage(
    private val htmlPageRenderer: HtmlPageRenderer,
    private val publicUrl: PublicUrl,
    private val getPorts: GetPorts,
) {
    @GetMapping("$PORTS_PREFIX${PortsSeo.SMALL_SCALE_PATH}")
    fun page(): ResponseEntity<String> {
        val baseUrl = publicUrl.get(Site.PORTS)

        return htmlPageRenderer.render(
            HtmlPage(
                title =
                    "La petite pêche côtière en chiffres : emplois, valeur ajoutée, limites | " +
                        Site.PORTS.siteName,
                description = DESCRIPTION,
                canonicalPath = PortsSeo.SMALL_SCALE_PATH,
                imageUrl = PortsSeo.homeImage(baseUrl),
                language = Language.FR,
                site = Site.PORTS,
                structuredData =
                    listOf(
                        PortsSeo.breadcrumb(baseUrl, "Petite pêche côtière" to PortsSeo.SMALL_SCALE_PATH),
                        mapOf(
                            "@context" to "https://schema.org",
                            "@type" to "Article",
                            "headline" to "La petite pêche côtière en France : emplois, valeur ajoutée et limites",
                            "description" to DESCRIPTION,
                            "inLanguage" to "fr",
                            "url" to baseUrl + PortsSeo.SMALL_SCALE_PATH,
                            "citation" to SOURCES.map { it.citation },
                        ),
                    ) + PortsSeo.faqStructuredData(QUESTIONS),
                body =
                    "<main>" + PortsSeo.nav("Petite pêche côtière" to null) +
                        "<h1>La petite pêche côtière en France : emplois, valeur ajoutée et limites</h1>" +
                        "<p>${escapeHtml(DESCRIPTION)}</p>" +
                        PortsSeo.updatedOn(getPorts.lastUpdate()) +
                        ContentCharts.tiles(TILES, "$AGRO ; moyennes 2017-2021") +
                        section("De quoi parle-t-on ?", DEFINITIONS) +
                        "<h2>Questions fréquentes</h2>" +
                        QUESTIONS
                            .mapIndexed { index, it ->
                                "<h3>${escapeHtml(it.question)}</h3><p>${escapeHtml(it.answer)}</p>" +
                                    CHARTS[index].orEmpty()
                            }.joinToString("") +
                        section("Les limites et les contreparties", LIMITS) +
                        "<h2>Sur la carte</h2><p>Pour chaque port, la carte donne la taille de ses navires (moins " +
                        "de 12 m, 12 à 24 m, 24 m et plus) et leurs engins de pêche, d’après le " +
                        """${PortsSeo.FLEET_REGISTER} : <a href="/">voir les ports</a>.</p>""" +
                        "<h2>Sources</h2><ul>" +
                        SOURCES.joinToString("") {
                            """<li><a href="${escapeHtml(it.url)}">${escapeHtml(it.citation)}</a>. """ +
                                "${escapeHtml(it.note)}</li>"
                        } + "</ul>" + links() + "</main>",
            ),
        )
    }

    private fun section(
        heading: String,
        paragraphs: List<String>,
    ) = "<h2>${escapeHtml(heading)}</h2>" + paragraphs.joinToString("") { "<p>${escapeHtml(it)}</p>" }

    private data class Source(
        val citation: String,
        val url: String,
        /** Who wrote it and who funded it, so that readers can weigh it */
        val note: String,
    )

    companion object {
        private const val AGRO = "Quemper, Levrel, Mouillard et Gascuel, L’Institut Agro, 2025"
        private const val PROFILE = "Frangoudes, Guyader et Le Grand, Ifremer – UMR AMURE, 2025"

        private const val DESCRIPTION =
            "Ce que la petite pêche côtière apporte à l’emploi et aux territoires littoraux, et ses limites, d’après " +
                "les travaux de recherche français publiés en 2025 : L’Institut Agro (programme TransiPêche) et le " +
                "profil national de la pêche française (Ifremer, UMR AMURE) pour la Commission européenne."

        private val DEFINITIONS =
            listOf(
                "Les deux sources ne délimitent pas la petite pêche de la même façon. Pour L’Institut Agro, les " +
                    "navires « côtiers » sont tous ceux de moins de 12 m, quel que soit leur engin ; ses chiffres " +
                    "sont des moyennes 2017-2021. Le profil national suit la définition européenne de la « petite " +
                    "pêche côtière » : les navires de moins de 12 m qui n’utilisent pas d’engin traînant (chalut, " +
                    "drague) ; ses chiffres portent sur 2022. Les parts qui en résultent diffèrent donc : chaque " +
                    "chiffre est donné ci-dessous avec sa source.",
                "Les « arts dormants » sont les engins posés, que le poisson vient rencontrer : filets, lignes, " +
                    "palangres, casiers. Les navires côtiers aux arts dormants, souvent appelés pêche artisanale, " +
                    "forment les trois quarts de la flotte française ($AGRO).",
            )

        private val QUESTIONS =
            listOf(
                Question(
                    "Combien de navires de petite pêche en France ?",
                    "Environ 5 220 navires de moins de 12 m, soit 84 % des quelque 6 200 navires de pêche français " +
                        "en activité, en moyenne sur 2017-2021 ; les trois quarts de la flotte sont des navires " +
                        "côtiers aux arts dormants (filets, lignes, casiers) ($AGRO).",
                ),
                Question(
                    "Combien d’emplois dans la petite pêche ?",
                    "Les navires de moins de 12 m font 42,5 % des 9 150 emplois embarqués en équivalent temps " +
                        "plein ; les seuls côtiers aux arts dormants en font 3 200, soit 35 %, dont " +
                        "1 700 en métropole ($AGRO). En 2022, la petite pêche côtière au sens européen employait 49 " +
                        "% des 12 463 marins embarqués, avec 2 emplois par navire en moyenne ($PROFILE).",
                ),
                Question(
                    "Quelles flottilles créent le plus d’emplois par tonne pêchée ?",
                    "Les arts dormants, quelle que soit la taille des navires : 20 à 40 emplois en équivalent temps " +
                        "plein pour 1 000 t débarquées en Atlantique, 75 à 100 en Méditerranée, contre 2 à 10 pour " +
                        "les chalutiers pélagiques industriels et les thoniers senneurs de plus de 40 m. Entre les " +
                        "fileyeurs de moins de 12 m et les chalutiers pélagiques de plus de 40 m, l’emploi par tonne " +
                        "pêchée est divisé par 16 ($AGRO).",
                ),
                Question(
                    "Quelle valeur ajoutée crée la petite pêche ?",
                    "Hors outre-mer, les côtiers aux arts dormants créent 23,4 % de la valeur ajoutée de la pêche " +
                        "française, alors qu’ils ne débarquent que 9 % des tonnages et 19 % de la valeur ; toutes " +
                        "tailles confondues, les arts dormants en créent 36,2 %. En Atlantique, un côtier aux arts " +
                        "dormants crée près de 3 000 € de valeur ajoutée par tonne, dix fois plus qu’un chalutier " +
                        "pélagique industriel (300 €/t), notamment parce que son poisson se vend plus cher ($AGRO).",
                ),
                Question(
                    "La petite pêche est-elle rentable sans subventions ?",
                    "Oui : les côtiers aux arts dormants sont la flottille qui dégage le plus d’excédent brut " +
                        "d’exploitation. Les navires aux arts dormants reçoivent des subventions, surtout la détaxe " +
                        "du gazole, inférieures à 0,6 fois leur excédent, et les côtiers sont ceux qui en dépendent " +
                        "le moins : 20 à 50 centimes d’aide par kilo débarqué, contre 40 à 80 centimes pour les " +
                        "chalutiers de fond de l’Atlantique. En Atlantique, les arts dormants côtiers et hauturiers " +
                        "créent 5 à 10 € de valeur ajoutée par euro de subvention, contre 1,3 à 2,5 € pour les " +
                        "chalutiers hauturiers et industriels ($AGRO).",
                ),
                Question(
                    "Pourquoi la petite pêche compte-t-elle pour les territoires littoraux ?",
                    "Ses navires débarquent dans la criée de leur port d’attache, quand les navires industriels " +
                        "débarquent souvent au port le plus proche de leur zone de pêche ; la vente directe y est " +
                        "plus courante, souvent assurée par les conjointes des pêcheurs. En Atlantique et mer du " +
                        "Nord, 98 % des marins de la petite pêche côtière sont français, contre 74 % sur les plus " +
                        "grands navires, et 60 des 98 femmes embarquées y travaillent ($PROFILE).",
                ),
            )

        private val TILES =
            listOf(
                ContentCharts.Tile("84 %", "des navires de pêche français ont moins de 12 m"),
                ContentCharts.Tile("42,5 %", "des emplois embarqués sont sur ces navires"),
                ContentCharts.Tile(
                    "× 16",
                    "d’emplois par tonne pêchée, d’un chalutier pélagique de plus de 40 m à un fileyeur de moins " +
                        "de 12 m",
                ),
                ContentCharts.Tile(
                    "× 10",
                    "de valeur ajoutée par tonne, des pélagiques industriels aux côtiers aux arts dormants, en " +
                        "Atlantique",
                ),
            )

        /** The chart after each question, by its index in [QUESTIONS] */
        private val CHARTS =
            mapOf(
                1 to
                    ContentCharts.shares(
                        "La flotte française par taille de navire, en % du total",
                        listOf("Moins de 12 m", "12 à 24 m", "24 m et plus"),
                        listOf(
                            "Navires" to listOf(84.3, 12.2, 3.4),
                            "Emplois" to listOf(42.5, 30.3, 27.3),
                            "Valeur débarquée" to listOf(28.7, 31.3, 39.9),
                            "Tonnage débarqué" to listOf(23.7, 27.8, 48.6),
                        ),
                        "$AGRO, tableaux 5 à 8 ; moyennes 2017-2021, outre-mer compris",
                    ),
                2 to
                    ContentCharts.ranges(
                        "Emplois pour 1 000 tonnes débarquées, en équivalent temps plein",
                        listOf(
                            ContentCharts.Range("Arts dormants, Méditerranée", 75.0, 100.0, true, "75 à 100"),
                            ContentCharts.Range("Arts dormants, Atlantique", 20.0, 40.0, true, "20 à 40"),
                            ContentCharts.Range("Chaluts de fond de moins de 18 m", 25.0, 25.0, false, "≈ 25"),
                            ContentCharts.Range("Grands chaluts de fond, dragues", 10.0, 10.0, false, "≈ 10"),
                            ContentCharts.Range(
                                "Chaluts pélagiques industriels, senneurs de plus de 40 m",
                                2.0,
                                10.0,
                                false,
                                "2 à 10",
                            ),
                        ),
                        axisMax = 100.0,
                        unit = "emplois pour 1 000 t",
                        source = "$AGRO, section 4.1",
                        note = "En foncé, les arts dormants : filets, lignes, palangres, casiers.",
                    ),
                3 to
                    ContentCharts.bars(
                        "Les côtiers aux arts dormants, hors outre-mer : leur part de la pêche française",
                        listOf(
                            "Tonnage débarqué" to 9.0,
                            "Valeur débarquée" to 19.0,
                            "Valeur ajoutée" to 23.4,
                        ),
                        "$AGRO, section 4.1 et tableau 9",
                    ) +
                    ContentCharts.ranges(
                        "Valeur ajoutée par tonne débarquée, en euros",
                        listOf(
                            ContentCharts.Range("Arts dormants, Méditerranée", 6000.0, 7000.0, true, "6 000 à 7 000 €"),
                            ContentCharts.Range("Arts dormants, Atlantique", 1500.0, 4500.0, true, "1 500 à 4 500 €"),
                            ContentCharts.Range("Chaluts de fond, dragues", 1000.0, 2500.0, false, "1 000 à 2 500 €"),
                            ContentCharts.Range(
                                "Plus de 40 m, chaluts pélagiques",
                                250.0,
                                1000.0,
                                false,
                                "250 à 1 000 €",
                            ),
                        ),
                        axisMax = 7000.0,
                        unit = "€ par tonne",
                        source = "$AGRO, section 4.1",
                        note =
                            "En foncé, les arts dormants. Hors thoniers industriels de Méditerranée (10 000 €/t), " +
                                "qui ne pêchent qu’une espèce de très grande valeur, le thon rouge.",
                    ),
                4 to
                    ContentCharts.ranges(
                        "Subventions par kilo débarqué, détaxe du gazole comprise, en euros",
                        listOf(
                            ContentCharts.Range("Arts dormants", 0.2, 0.5, true, "0,20 à 0,50 €"),
                            ContentCharts.Range("Chaluts pélagiques", 0.1, 0.3, false, "0,10 à 0,30 €"),
                            ContentCharts.Range(
                                "Chaluts et sennes de fond, Atlantique",
                                0.4,
                                0.8,
                                false,
                                "0,40 à 0,80 €",
                            ),
                            ContentCharts.Range("Chaluts de fond, Méditerranée", 1.6, 1.6, false, "jusqu’à 1,60 €"),
                        ),
                        axisMax = 1.6,
                        unit = "€ par kg",
                        source = "$AGRO, section 4.3",
                        note = "En foncé, les arts dormants.",
                    ),
            )

        private val LIMITS =
            listOf(
                "Des salaires plus bas : le coût salarial par marin est de 40 à 70 k€ par an sur les navires aux " +
                    "arts dormants, pour une moyenne de 72 k€ toutes flottilles confondues ; les côtiers de " +
                    "Méditerranée et les fileyeurs et ligneurs industriels ont les plus faibles ($AGRO).",
                "Une petite production par navire : en Méditerranée, un navire de petite pêche côtière débarque en " +
                    "moyenne 6 t par an pour 53 000 € de chiffre d’affaires, en Atlantique 61 t pour 156 000 € " +
                    "($PROFILE).",
                "Un secteur qui recule et vieillit : en Atlantique et mer du Nord, l’emploi de la petite pêche " +
                    "côtière a baissé de 14,6 % entre 2011 et 2022, et 51 % de ses navires ont plus de 30 ans " +
                    "($PROFILE).",
                "Tous les petits navires ne sont pas sobres : par tonne pêchée, un petit chalutier de fond racle " +
                    "plus de fond marin qu’un grand (environ 1,5 km² par tonne contre 0,1 km²) ($AGRO).",
                "Des données moins complètes : en Atlantique et mer du Nord, les données sociales ne couvrent " +
                    "que 76 % des navires de moins de 10 m, contre 92 % des autres ($PROFILE) ; L’Institut Agro " +
                    "souligne la forte incertitude de plusieurs de ses indicateurs, faute de données à l’échelle " +
                    "de toutes les flottilles.",
            )

        private val SOURCES =
            listOf(
                Source(
                    "Quemper F., Levrel H., Mouillard R., Gascuel D. (2025). Évaluation des performances " +
                        "environnementales, économiques et sociales des flottilles de pêche : Bilan France. Les " +
                        "publications du Pôle halieutique, mer et littoral de L’Institut Agro n° 56",
                    "https://halieutique.institut-agro.fr/files/fichiers/pdf/performances.pdf",
                    "Rapport de recherche du programme TransiPêche, financé par la Fondation 2050 et par " +
                        "l’association BLOOM ; données publiques 2017-2021.",
                ),
                Source(
                    "Frangoudes K., Guyader O., Le Grand C. (2025). National Fisheries Profile of France: a detailed " +
                        "social analysis of the French fishing sector. Commission européenne, DG MARE",
                    "https://op.europa.eu/en/publication-detail/-/publication/b9420cee-a32b-11f0-97c8-01aa75ed71a1",
                    "Rapport d’Ifremer et de l’UMR AMURE (Université de Bretagne Occidentale) pour la Commission " +
                        "européenne, rédigé en 2024 et publié en octobre 2025 ; données 2022.",
                ),
            )
    }
}
