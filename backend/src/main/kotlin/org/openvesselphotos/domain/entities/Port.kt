package org.openvesselphotos.domain.entities

data class PortLocation(
    val latitude: Double,
    val longitude: Double,
)

/** A port of registration of the EU fleet register, with how many of its vessels are photographed. */
data class Port(
    /** UN/LOCODE, e.g. FRBES */
    val code: String,
    val name: String,
    /** Unknown for the few codes missing from UN/LOCODE */
    val location: PortLocation?,
    val vessels: Int,
    val photographed: Int,
) {
    /** ISO 3166-1 alpha-2 code, e.g. FR, or MQ for Martinique */
    val country: String get() = code.take(2)

    companion object {
        private val CODE = Regex("[A-Z]{2}[A-Z0-9]{3}")

        fun isCode(value: String): Boolean = CODE.matches(value)

        /** Words written in lower case inside French names: the register writes "Fort-De-France", "Sables-D'Olonne". */
        private val LOWER_CASE_WORDS =
            setOf("de", "du", "des", "en", "sur", "sous", "la", "le", "les", "aux", "au", "et")

        /**
         * "Fort-De-France" -> "Fort-de-France", "Noirmoutier-En-L'Île" -> "Noirmoutier-en-l'Île",
         * "Pointe-A-Pitre" -> "Pointe-à-Pitre".
         */
        fun prettyName(name: String): String =
            Regex("(?<=[- ])([\\p{L}]+)('?)").replace(name.trim()) { match ->
                val word = match.groupValues[1]
                val isElided = match.groupValues[2] == "'" && word.length == 1
                when {
                    word == "A" && match.groupValues[2].isEmpty() -> "à"
                    word.lowercase() in LOWER_CASE_WORDS || isElided -> match.value.lowercase()
                    else -> match.value
                }
            }
    }
}
