package org.openvesselphotos.infrastructure.web

/** Names of the FAO codes of the EU fleet register, as in the React app (frontend/src/i18n/en.ts and fr.ts). */
object FaoCodes {
    /** ISSCFV vessel types */
    private val ENGLISH_VESSEL_TYPES =
        mapOf(
            "DO" to "Dredger",
            "GO" to "Gillnetter",
            "LL" to "Longliner",
            "LO" to "Liner",
            "LP" to "Pole and line vessel",
            "MO" to "Multipurpose vessel",
            "NO" to "Lift netter",
            "SO" to "Seiner",
            "TO" to "Trawler",
            "TT" to "Stern trawler",
            "WO" to "Trap setter",
        )

    /** ISSCFG gears */
    private val ENGLISH_GEARS =
        mapOf(
            "DRB" to "Boat dredges",
            "DRH" to "Hand dredges",
            "DRM" to "Mechanised dredges",
            "FIX" to "Traps",
            "FPO" to "Pots",
            "FYK" to "Fyke nets",
            "GEN" to "Gillnets and entangling nets",
            "GN" to "Gillnets",
            "GNC" to "Encircling gillnets",
            "GND" to "Drift gillnets",
            "GNF" to "Fixed gillnets (on stakes)",
            "GNS" to "Set gillnets",
            "GTN" to "Combined gillnets and trammel nets",
            "GTR" to "Trammel nets",
            "HMX" to "Harvesting machines",
            "LA" to "Lampara nets",
            "LHM" to "Mechanised handlines and pole-lines",
            "LHP" to "Handlines and pole-lines",
            "LL" to "Longlines",
            "LLD" to "Drifting longlines",
            "LLS" to "Set longlines",
            "LN" to "Lift nets",
            "LNB" to "Boat-operated lift nets",
            "LNS" to "Shore-operated lift nets",
            "LTL" to "Trolling lines",
            "LVT" to "Vertical lines",
            "LX" to "Hooks and lines",
            "MDV" to "Diving",
            "MHI" to "Hand implements",
            "MIS" to "Miscellaneous gear",
            "OT" to "Otter trawls",
            "OTB" to "Bottom otter trawls",
            "OTM" to "Midwater otter trawls",
            "OTT" to "Otter twin trawls",
            "PS" to "Purse seines",
            "PS1" to "One-boat purse seines",
            "PT" to "Pair trawls",
            "PTB" to "Bottom pair trawls",
            "PTM" to "Midwater pair trawls",
            "SB" to "Beach seines",
            "SDN" to "Danish seines",
            "SPR" to "Pair seines",
            "SSC" to "Scottish seines",
            "SX" to "Seine nets",
            "TB" to "Bottom trawls",
            "TBB" to "Beam trawls",
            "TBS" to "Bottom shrimp trawls",
            "TM" to "Midwater trawls",
            "TMS" to "Midwater shrimp trawls",
            "TX" to "Other trawls",
        )

    private val FRENCH_VESSEL_TYPES =
        mapOf(
            "DO" to "Dragueur",
            "GO" to "Fileyeur",
            "LL" to "Palangrier",
            "LO" to "Ligneur",
            "LP" to "Canneur",
            "MO" to "Navire polyvalent",
            "NO" to "Navire de pêche au filet soulevé",
            "SO" to "Senneur",
            "TO" to "Chalutier",
            "TT" to "Chalutier pêche arrière",
            "WO" to "Caseyeur",
        )

    private val FRENCH_GEARS =
        mapOf(
            "DRB" to "Dragues remorquées",
            "DRH" to "Dragues à main",
            "DRM" to "Dragues mécanisées",
            "FIX" to "Pièges",
            "FPO" to "Casiers",
            "FYK" to "Verveux",
            "GEN" to "Filets maillants et emmêlants",
            "GN" to "Filets maillants",
            "GNC" to "Filets maillants encerclants",
            "GND" to "Filets maillants dérivants",
            "GNF" to "Filets maillants fixes (sur perches)",
            "GNS" to "Filets maillants calés",
            "GTN" to "Trémails et filets maillants combinés",
            "GTR" to "Trémails",
            "HMX" to "Machines de récolte",
            "LA" to "Filets lamparo",
            "LHM" to "Lignes à main et cannes mécanisées",
            "LHP" to "Lignes à main et cannes",
            "LL" to "Palangres",
            "LLD" to "Palangres dérivantes",
            "LLS" to "Palangres calées",
            "LN" to "Filets soulevés",
            "LNB" to "Filets soulevés manœuvrés du bateau",
            "LNS" to "Filets soulevés manœuvrés du rivage",
            "LTL" to "Lignes de traîne",
            "LVT" to "Lignes verticales",
            "LX" to "Hameçons et lignes",
            "MDV" to "Plongée",
            "MHI" to "Engins à main",
            "MIS" to "Engins divers",
            "OT" to "Chaluts à panneaux",
            "OTB" to "Chaluts de fond à panneaux",
            "OTM" to "Chaluts pélagiques à panneaux",
            "OTT" to "Chaluts jumeaux à panneaux",
            "PS" to "Sennes coulissantes",
            "PS1" to "Sennes coulissantes à un bateau",
            "PT" to "Chaluts bœufs",
            "PTB" to "Chaluts bœufs de fond",
            "PTM" to "Chaluts bœufs pélagiques",
            "SB" to "Sennes de plage",
            "SDN" to "Sennes danoises",
            "SPR" to "Sennes à deux bateaux",
            "SSC" to "Sennes écossaises",
            "SX" to "Sennes",
            "TB" to "Chaluts de fond",
            "TBB" to "Chaluts à perche",
            "TBS" to "Chaluts de fond à crevettes",
            "TM" to "Chaluts pélagiques",
            "TMS" to "Chaluts pélagiques à crevettes",
            "TX" to "Autres chaluts",
        )

    /** e.g. "Gillnetter" for GO; null for FX (not specified) and unknown codes */
    fun vesselType(
        code: String,
        language: Language,
    ): String? =
        when (language) {
            Language.EN -> ENGLISH_VESSEL_TYPES
            Language.FR -> FRENCH_VESSEL_TYPES
        }[code]

    /** e.g. "Bottom otter trawls" for OTB, the code itself when unknown */
    fun gear(
        code: String,
        language: Language,
    ): String =
        when (language) {
            Language.EN -> ENGLISH_GEARS
            Language.FR -> FRENCH_GEARS
        }[code] ?: code
}
