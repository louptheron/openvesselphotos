package org.openvesselphotos.domain.entities

import java.time.LocalDate

/** The websites served by this app, whose page views are counted apart */
enum class Website {
    OPEN_VESSEL_PHOTOS,

    /** La Pêche française, the map of the French fishing vessels by home port */
    LA_PECHE_FRANCAISE,
    ;

    /** The source of the images and API calls made by the pages of this website, see [UsageKey] */
    val usageSource: String
        get() =
            when (this) {
                OPEN_VESSEL_PHOTOS -> UsageKey.WEBSITE
                LA_PECHE_FRANCAISE -> UsageKey.PORTS_WEBSITE
            }
}

data class PageViewKey(
    val day: LocalDate,
    val site: Website,
    /** The path of the page, e.g. "/vessels/cfr/FRA000123456", or [UsageKey.OTHER] */
    val path: String,
)

data class PageViewCount(
    val key: PageViewKey,
    val count: Long,
)
