package org.openvesselphotos.infrastructure.web

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What the pages of the ports site share for search engines and AI assistants: questions answered in one sourced
 * sentence (FAQPage), the date of the data, and the sources (see PortsContentController.methodology).
 */
object PortsSeo {
    const val TAGLINE = "chiffres et données de la pêche professionnelle"
    const val FRANCE_AGRIMER = "FranceAgriMer, ventes déclarées en halles à marée"
    const val FRANCE_AGRIMER_URL = "https://www.franceagrimer.fr"
    const val FLEET_REGISTER = "registre de la flotte de pêche de l’Union européenne"
    const val FLEET_REGISTER_URL = "https://webgate.ec.europa.eu/fleet-europa/search_en"
    const val GFW_URL = "https://globalfishingwatch.org"
    const val METHODOLOGY_PATH = "/methodologie"
    const val SPECIES_PATH = "/especes"
    const val PORTS_CSV_PATH = "/donnees/ports.csv"
    const val SALES_CSV_PATH = "/donnees/criees.csv"

    fun portPath(slug: String) = "/ports/$slug"

    fun speciesPath(slug: String) = "$SPECIES_PATH/$slug"

    data class Question(
        val question: String,
        /** One sentence, with its figure, its source and its year */
        val answer: String,
    )

    /** e.g. "20 novembre 2026" */
    fun date(day: LocalDate): String = day.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE))

    fun faqSection(questions: List<Question>): String =
        if (questions.isEmpty()) {
            ""
        } else {
            "<section><h2>Questions fréquentes</h2>" +
                questions.joinToString("") { "<h3>${escapeHtml(it.question)}</h3><p>${escapeHtml(it.answer)}</p>" } +
                "</section>"
        }

    fun faqStructuredData(questions: List<Question>): List<Map<String, Any?>> =
        if (questions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                mapOf(
                    "@context" to "https://schema.org",
                    "@type" to "FAQPage",
                    "mainEntity" to
                        questions.map {
                            mapOf(
                                "@type" to "Question",
                                "name" to it.question,
                                "acceptedAnswer" to mapOf("@type" to "Answer", "text" to it.answer),
                            )
                        },
                ),
            )
        }

    /** "Mis à jour le…": search engines and AI assistants favour pages that say how fresh their figures are */
    fun updatedOn(day: LocalDate?): String =
        day?.let { """<p>Mis à jour le <time datetime="$it">${date(it)}</time>.</p>""" } ?: ""

    fun breadcrumb(
        baseUrl: String,
        vararg items: Pair<String, String>,
    ): Map<String, Any?> =
        mapOf(
            "@context" to "https://schema.org",
            "@type" to "BreadcrumbList",
            "itemListElement" to
                (listOf(Site.PORTS.siteName to "/") + items).mapIndexed { index, (name, path) ->
                    mapOf("@type" to "ListItem", "position" to index + 1, "name" to name, "item" to baseUrl + path)
                },
        )

    fun nav(vararg items: Pair<String, String?>): String =
        """<nav><a href="/">${Site.PORTS.siteName}</a>""" +
            items.joinToString("") { (name, path) ->
                " › " + (path?.let { """<a href="$it">${escapeHtml(name)}</a>""" } ?: escapeHtml(name))
            } + "</nav>"

    /** The figures of the site as a dataset, with its CSV downloads: Google Dataset Search indexes them */
    fun dataset(
        baseUrl: String,
        updatedOn: LocalDate?,
        auctionYear: Int?,
    ): Map<String, Any?> =
        mapOf(
            "@context" to "https://schema.org",
            "@type" to "Dataset",
            "name" to "La pêche professionnelle en France : flotte par port et ventes des criées",
            "description" to
                "Les navires de pêche professionnelle français par port d’attache, par engin de pêche et par taille " +
                "($FLEET_REGISTER, positions des navires), et les ventes de chaque criée par espèce : tonnages, " +
                "valeurs et prix moyens ($FRANCE_AGRIMER).",
            "url" to baseUrl + METHODOLOGY_PATH,
            "inLanguage" to "fr",
            "isAccessibleForFree" to true,
            "keywords" to
                listOf(
                    "pêche professionnelle",
                    "flotte de pêche française",
                    "ports de pêche",
                    "criées",
                    "débarquements",
                    "prix du poisson",
                ),
            "creator" to mapOf("@type" to "Organization", "name" to Site.PORTS.siteName, "url" to "$baseUrl/"),
            "spatialCoverage" to mapOf("@type" to "Place", "name" to "France"),
            "temporalCoverage" to auctionYear?.let { "${it - 1}/$it" },
            "dateModified" to updatedOn?.toString(),
            "isBasedOn" to listOf(FRANCE_AGRIMER_URL, FLEET_REGISTER_URL, GFW_URL),
            "distribution" to
                listOf(
                    mapOf(
                        "@type" to "DataDownload",
                        "name" to "Les ports de pêche et leur flotte",
                        "encodingFormat" to "text/csv",
                        "contentUrl" to baseUrl + PORTS_CSV_PATH,
                    ),
                    mapOf(
                        "@type" to "DataDownload",
                        "name" to "Les ventes des criées par espèce",
                        "encodingFormat" to "text/csv",
                        "contentUrl" to baseUrl + SALES_CSV_PATH,
                    ),
                ),
        ).filterValues { it != null }
}
