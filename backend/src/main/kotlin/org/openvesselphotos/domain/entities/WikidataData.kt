package org.openvesselphotos.domain.entities

import java.time.LocalDate

/** What Wikidata (CC0) knows of a vessel, found by its MMSI: mostly for vessels outside the EU fleet register. */
data class WikidataData(
    /** e.g. "Q7868346" */
    val id: String? = null,
    val imo: String? = null,
    /** e.g. "replenishment oiler" */
    val shipTypeEn: String? = null,
    /** e.g. "pétrolier ravitailleur" */
    val shipTypeFr: String? = null,
    /** Year of construction, or else of entry into service */
    val yearBuilt: Int? = null,
    val lengthMeters: Double? = null,
    /** Names the vessel had or has, other than its name here, e.g. a former name */
    val otherNames: List<String> = emptyList(),
    /** Day of the last import that gave this data */
    val importedOn: LocalDate? = null,
) {
    val isEmpty: Boolean get() = id == null

    val url: String? get() = id?.let { "https://www.wikidata.org/wiki/$it" }

    fun shipType(languageCode: String): String? = if (languageCode == "fr") shipTypeFr ?: shipTypeEn else shipTypeEn
}
