package org.openvesselphotos.domain.entities

import java.time.LocalDate

enum class UsageKind {
    /** A vessel photo loaded, e.g. by an `<img>` on another site */
    IMAGE,

    /** Any other call to the public API */
    API,
}

data class UsageKey(
    val day: LocalDate,
    val kind: UsageKind,
    /** [WEBSITE], [DIRECT] or the host of the site the request comes from */
    val source: String,
    val apiKeyId: Int?,
) {
    companion object {
        /** Requests from this website's own pages */
        const val WEBSITE = "website"

        /** Requests without a Referer: apps, scripts, or browsers that do not send it */
        const val DIRECT = "direct"

        /** Sites beyond the maximum number counted at once, so that forged Referer headers cannot fill the database */
        const val OTHER = "other"
    }
}

data class UsageCount(
    val key: UsageKey,
    val count: Long,
)
