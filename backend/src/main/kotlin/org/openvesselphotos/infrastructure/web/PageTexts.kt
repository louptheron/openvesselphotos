package org.openvesselphotos.infrastructure.web

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Texts of the server-rendered pages, as in the React app (frontend/src/i18n/en.ts and fr.ts). */
sealed interface PageTexts {
    val homeTitle: String
    val homeDescription: String
    val homeHeading: String
    val recentlyAdded: String
    val allVessels: String
    val contributePhoto: String
    val apiDocumentation: String
    val search: String
    val searchVesselPhotos: String
    val pageNotFound: String
    val addPhoto: String
    val source: String
    val vesselPhotos: String
    val otherVessels: String
    val previousPage: String
    val nextPage: String
    val allVesselsWithPhotos: String
    val contributeTitle: String
    val contributeDescription: String
    val contributeHeading: String
    val contributeText: String
    val docsTitle: String
    val docsDescription: String
    val docsHeading: String
    val docsText: String
    val apiKeyTitle: String
    val apiKeyDescription: String
    val apiKeyHeading: String
    val apiKeyText: String

    fun photos(count: Int): String = "$count photo${if (count == 1) "" else "s"}"

    fun noVesselYet(key: String): String

    fun flag(country: String): String

    /** e.g. "Flag France", after the vessel's keys */
    fun flagFact(country: String): String

    /** e.g. "Also known as Lee A. Tregurtha" */
    fun otherNames(names: String): String

    /** Credits Wikidata, linked, for the data found there */
    val wikidataSource: String

    /** [summary]: the vessel's own sentence (see [summary]), which opens the description of a vessel without photo */
    fun vesselDescription(
        name: String,
        details: String,
        photoCount: Int,
        licences: String,
        summary: String?,
    ): String

    fun otherVesselsOf(port: String): String

    fun taken(date: LocalDate): String

    /** e.g. "LOLA (LR 539755), gillnetter, at La Rochelle, by Jean Dupont" */
    fun photoAlt(
        vessel: String,
        vesselType: String?,
        place: String?,
        credit: String,
    ): String

    /** e.g. "LOLA, by Jean Dupont" */
    fun photoCaption(
        vessel: String,
        credit: String,
    ): String

    fun formatNumber(value: Double): String

    /** The vessel's own sentence, e.g. "LOLA is a gillnetter of 14.85 m, built in 1981, registered in La Rochelle…" */
    fun summary(
        name: String,
        vesselType: String?,
        lengthMeters: Double?,
        yearBuilt: Int?,
        port: String?,
        country: String?,
        mainGear: String?,
    ): String

    fun registerFacts(
        yearBuilt: Int?,
        lengthMeters: Double?,
        enginePowerKw: Double?,
        tonnageGt: Double?,
        mainGear: String?,
        callSign: String?,
    ): String?

    fun filterFacts(
        country: String?,
        port: String?,
        vesselType: String?,
        mainGear: String?,
    ): String

    fun vesselsWithPhotos(filterText: String): String

    fun pageOf(
        number: Int,
        count: Int,
    ): String

    /** e.g. ", page 2 of 5 (ALBATROS to BELLE-ÎLE)" */
    fun pageRange(
        number: Int,
        count: Int,
        first: String?,
        last: String?,
    ): String

    fun directoryDescription(
        total: Int,
        pageText: String,
    ): String

    data object English : PageTexts {
        override val homeTitle = "Open Vessel Photos — free photos of any vessel, by CFR or MMSI"
        override val homeDescription =
            "Free, community-built library of vessel photos, searchable by CFR or MMSI. " +
                "Every photo comes with its credit and an open licence, and a free API."
        override val homeHeading = "The open library of vessel photos, by CFR or MMSI."
        override val recentlyAdded = "Recently added"
        override val allVessels = "All vessels"
        override val contributePhoto = "Contribute a photo"
        override val apiDocumentation = "API documentation"
        override val search = "Search"
        override val searchVesselPhotos = "Search vessel photos"
        override val pageNotFound = "Page not found"
        override val addPhoto = "Add a photo"
        override val source = "Source"
        override val vesselPhotos = "vessel photos"
        override val otherVessels = "Other vessels"
        override val previousPage = "Previous page"
        override val nextPage = "Next page"
        override val allVesselsWithPhotos = "All vessels with photos"
        override val contributeTitle = "Contribute a vessel photo"
        override val contributeDescription =
            "Share your own photo of a vessel under an open licence (CC BY, CC BY-SA or CC0), with its CFR or MMSI. " +
                "Published once reviewed by a volunteer."
        override val contributeHeading = "Contribute a photo"
        override val contributeText = "Your own picture, an open licence, and the vessel's CFR or MMSI."
        override val docsTitle = "Free vessel photo API, by CFR or MMSI"
        override val docsDescription =
            "Free JSON and image API for vessel photos: one URL per vessel, by CFR or MMSI, with credit and licence. " +
                "No API key, CORS enabled."
        override val docsHeading = "The vessel photo API"
        override val docsText = "CFR and MMSI are both first-class keys:"
        override val apiKeyTitle = "Request an API key"
        override val apiKeyDescription =
            "Request a free API key to contribute vessel photos, suggest CFR or MMSI keys and report photos " +
                "with scripts. Reading the API needs no key."
        override val apiKeyHeading = "Request an API key"
        override val apiKeyText =
            "Reading the API is free and open. An API key is only needed to send data with a script."

        override fun noVesselYet(key: String) = "No vessel with $key yet"

        override fun flag(country: String) = "flag $country"

        override fun flagFact(country: String) = "Flag $country"

        override fun otherNames(names: String) = "Also known as $names"

        override val wikidataSource = "Vessel data from Wikidata (CC0)"

        override fun vesselDescription(
            name: String,
            details: String,
            photoCount: Int,
            licences: String,
            summary: String?,
        ) = if (photoCount == 0) {
            "${summary ?: "Vessel $name."} ${details.replaceFirstChar { it.uppercase() }}. " +
                "No photo yet: contribute the first one."
        } else {
            "${photos(photoCount)} of the vessel $name ($details), free to reuse under open licences ($licences)."
        }

        override fun otherVesselsOf(port: String) = "Other vessels of $port"

        override fun taken(date: LocalDate) = "taken $date"

        override fun photoAlt(
            vessel: String,
            vesselType: String?,
            place: String?,
            credit: String,
        ) = listOfNotNull(vessel, vesselType?.lowercase(), place?.let { "at $it" }).joinToString(", ") +
            ", by $credit"

        override fun photoCaption(
            vessel: String,
            credit: String,
        ) = "$vessel, by $credit"

        override fun formatNumber(value: Double): String = value.toBigDecimal().stripTrailingZeros().toPlainString()

        override fun summary(
            name: String,
            vesselType: String?,
            lengthMeters: Double?,
            yearBuilt: Int?,
            port: String?,
            country: String?,
            mainGear: String?,
        ) = "$name is a ${vesselType?.lowercase() ?: "vessel"}" +
            lengthMeters?.let { " of ${formatNumber(it)} m" }.orEmpty() +
            yearBuilt?.let { ", built in $it" }.orEmpty() +
            listOfNotNull(port?.let { "registered in $it" }, country?.let { "under the flag of $it" })
                .joinToString(" ")
                .ifEmpty { null }
                ?.let { ", $it" }
                .orEmpty() +
            "." + mainGear?.let { " Main gear: ${it.lowercase()}." }.orEmpty()

        override fun registerFacts(
            yearBuilt: Int?,
            lengthMeters: Double?,
            enginePowerKw: Double?,
            tonnageGt: Double?,
            mainGear: String?,
            callSign: String?,
        ) = listOfNotNull(
            yearBuilt?.let { "Built in $it" },
            lengthMeters?.let { "${formatNumber(it)} m" },
            enginePowerKw?.let { "${formatNumber(it)} kW" },
            tonnageGt?.let { "${formatNumber(it)} GT" },
            mainGear?.let { "main gear $it" },
            callSign?.let { "call sign $it" },
        ).takeIf { it.isNotEmpty() }?.joinToString(" · ", postfix = " (EU fleet register)")

        override fun filterFacts(
            country: String?,
            port: String?,
            vesselType: String?,
            mainGear: String?,
        ) = listOfNotNull(
            country?.let { flag(it) },
            port?.let { "port $it" },
            vesselType?.let { "type $it" },
            mainGear?.let { "main gear $it" },
        ).joinToString(", ")

        override fun vesselsWithPhotos(filterText: String) = "Vessels with photos: $filterText"

        override fun pageOf(
            number: Int,
            count: Int,
        ) = "page $number of $count"

        override fun pageRange(
            number: Int,
            count: Int,
            first: String?,
            last: String?,
        ) = ", ${pageOf(number, count)}${if (first != null) " ($first to $last)" else ""}"

        override fun directoryDescription(
            total: Int,
            pageText: String,
        ) = "Browse the $total vessels with free photos on ${HtmlPageRenderer.SITE_NAME}, by name$pageText."
    }

    data object French : PageTexts {
        /** Before ":" and other double punctuation in French */
        private const val NBSP = " "

        override val homeTitle = "Open Vessel Photos — photos libres de n’importe quel navire, par CFR ou MMSI"
        override val homeDescription =
            "Photothèque libre et collaborative de navires, consultable par CFR ou MMSI. " +
                "Chaque photo est fournie avec son crédit et une licence libre, ainsi qu’une API gratuite."
        override val homeHeading = "La photothèque libre des navires, par CFR ou MMSI."
        override val recentlyAdded = "Ajoutés récemment"
        override val allVessels = "Tous les navires"
        override val contributePhoto = "Envoyer une photo"
        override val apiDocumentation = "Documentation de l’API"
        override val search = "Recherche"
        override val searchVesselPhotos = "Rechercher des photos de navires"
        override val pageNotFound = "Page introuvable"
        override val addPhoto = "Ajouter une photo"
        override val source = "Source"
        override val vesselPhotos = "photos du navire"
        override val otherVessels = "Autres navires"
        override val previousPage = "Page précédente"
        override val nextPage = "Page suivante"
        override val allVesselsWithPhotos = "Tous les navires avec photos"
        override val contributeTitle = "Envoyer une photo de navire"
        override val contributeDescription =
            "Partagez votre propre photo d’un navire sous licence libre (CC BY, CC BY-SA ou CC0), avec son CFR ou " +
                "son MMSI. Publiée après relecture par un bénévole."
        override val contributeHeading = "Envoyer une photo"
        override val contributeText = "Votre propre photo, une licence libre, et le CFR ou le MMSI du navire."
        override val docsTitle = "API gratuite de photos de navires, par CFR ou MMSI"
        override val docsDescription =
            "API JSON et images gratuite de photos de navires$NBSP: une URL par navire, par CFR ou MMSI, avec crédit " +
                "et licence. Sans clé d’API, CORS activé."
        override val docsHeading = "L’API de photos de navires"
        override val docsText = "Le CFR et le MMSI sont deux clés à part entière$NBSP:"
        override val apiKeyTitle = "Demander une clé d’API"
        override val apiKeyDescription =
            "Demandez une clé d’API gratuite pour envoyer des photos de navires, proposer des CFR ou MMSI et " +
                "signaler des photos par script. Lire l’API ne demande aucune clé."
        override val apiKeyHeading = "Demander une clé d’API"
        override val apiKeyText =
            "Lire l’API est libre et gratuit. Une clé d’API ne sert qu’à envoyer des données par script."

        private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        override fun noVesselYet(key: String) = "Aucun navire avec le $key pour l’instant"

        override fun flag(country: String) = "pavillon $country"

        override fun flagFact(country: String) = "Pavillon $country"

        override fun otherNames(names: String) = "Aussi connu sous le nom de $names"

        override val wikidataSource = "Données du navire$NBSP: Wikidata (CC0)"

        override fun vesselDescription(
            name: String,
            details: String,
            photoCount: Int,
            licences: String,
            summary: String?,
        ) = if (photoCount == 0) {
            "${summary ?: "Navire $name."} ${details.replaceFirstChar { it.uppercase() }}. " +
                "Pas encore de photo$NBSP: envoyez la première."
        } else {
            "${photos(photoCount)} du navire $name ($details), réutilisable${if (photoCount == 1) "" else "s"} " +
                "librement sous licence libre ($licences)."
        }

        override fun otherVesselsOf(port: String) = "Autres navires de $port"

        override fun taken(date: LocalDate) = "prise le ${date.format(dateFormatter)}"

        override fun photoAlt(
            vessel: String,
            vesselType: String?,
            place: String?,
            credit: String,
        ) = listOfNotNull(vessel, vesselType?.lowercase(), place?.let { "à $it" }).joinToString(", ") +
            ", par $credit"

        override fun photoCaption(
            vessel: String,
            credit: String,
        ) = "$vessel, par $credit"

        override fun formatNumber(value: Double): String = English.formatNumber(value).replace('.', ',')

        override fun summary(
            name: String,
            vesselType: String?,
            lengthMeters: Double?,
            yearBuilt: Int?,
            port: String?,
            country: String?,
            mainGear: String?,
        ) = "$name est un ${vesselType?.lowercase() ?: "navire"}" +
            lengthMeters?.let { " de ${formatNumber(it)}${NBSP}m" }.orEmpty() +
            yearBuilt?.let { ", construit en $it" }.orEmpty() +
            port?.let { ", immatriculé à $it" }.orEmpty() +
            country?.let { ", pavillon$NBSP: $it" }.orEmpty() +
            "." + mainGear?.let { " Engin principal$NBSP: ${it.lowercase()}." }.orEmpty()

        override fun registerFacts(
            yearBuilt: Int?,
            lengthMeters: Double?,
            enginePowerKw: Double?,
            tonnageGt: Double?,
            mainGear: String?,
            callSign: String?,
        ) = listOfNotNull(
            yearBuilt?.let { "Construit en $it" },
            lengthMeters?.let { "${formatNumber(it)}${NBSP}m" },
            enginePowerKw?.let { "${formatNumber(it)}${NBSP}kW" },
            tonnageGt?.let { "${formatNumber(it)}${NBSP}UMS" },
            mainGear?.let { "engin principal $it" },
            callSign?.let { "indicatif d’appel $it" },
        ).takeIf { it.isNotEmpty() }?.joinToString(" · ", postfix = " (registre de la flotte de l’UE)")

        override fun filterFacts(
            country: String?,
            port: String?,
            vesselType: String?,
            mainGear: String?,
        ) = listOfNotNull(
            country?.let { flag(it) },
            port?.let { "quartier $it" },
            vesselType?.let { "type $it" },
            mainGear?.let { "engin principal $it" },
        ).joinToString(", ")

        override fun vesselsWithPhotos(filterText: String) = "Navires avec photos$NBSP: $filterText"

        override fun pageOf(
            number: Int,
            count: Int,
        ) = "page $number sur $count"

        override fun pageRange(
            number: Int,
            count: Int,
            first: String?,
            last: String?,
        ) = ", ${pageOf(number, count)}${if (first != null) " (de $first à $last)" else ""}"

        override fun directoryDescription(
            total: Int,
            pageText: String,
        ) = "Parcourez les $total navires avec photos libres sur ${HtmlPageRenderer.SITE_NAME}, par nom$pageText."
    }

    companion object {
        fun of(language: Language): PageTexts =
            when (language) {
                Language.EN -> English
                Language.FR -> French
            }
    }
}
