package org.openvesselphotos.domain.entities

import java.text.Normalizer

/** The words of a name in a URL, e.g. "Le Guilvinec" → "le-guilvinec", "Coquille St-Jacques" → "coquille-st-jacques" */
fun slugOf(name: String): String =
    Normalizer
        .normalize(name, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()
        .replace("œ", "oe")
        .replace("æ", "ae")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
