package org.openvesselphotos.domain.entities

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException

enum class Licence(
    /** SPDX identifier, used in the API. */
    val spdx: String,
    val label: String,
    val requiresCredit: Boolean,
    val url: String,
) {
    CC0("CC0-1.0", "CC0", false, "https://creativecommons.org/publicdomain/zero/1.0/"),
    CC_BY_4_0("CC-BY-4.0", "CC BY 4.0", true, "https://creativecommons.org/licenses/by/4.0/"),
    CC_BY_SA_4_0("CC-BY-SA-4.0", "CC BY-SA 4.0", true, "https://creativecommons.org/licenses/by-sa/4.0/"),

    // Older versions and the public domain, for photos imported from Wikimedia Commons or Flickr: they keep their own
    // licence, which cannot be upgraded to 4.0 (a contributor's own photo is published under one of the above)
    CC_BY_3_0("CC-BY-3.0", "CC BY 3.0", true, "https://creativecommons.org/licenses/by/3.0/"),
    CC_BY_2_5("CC-BY-2.5", "CC BY 2.5", true, "https://creativecommons.org/licenses/by/2.5/"),
    CC_BY_2_0("CC-BY-2.0", "CC BY 2.0", true, "https://creativecommons.org/licenses/by/2.0/"),
    CC_BY_1_0("CC-BY-1.0", "CC BY 1.0", true, "https://creativecommons.org/licenses/by/1.0/"),
    CC_BY_SA_3_0("CC-BY-SA-3.0", "CC BY-SA 3.0", true, "https://creativecommons.org/licenses/by-sa/3.0/"),
    CC_BY_SA_2_5("CC-BY-SA-2.5", "CC BY-SA 2.5", true, "https://creativecommons.org/licenses/by-sa/2.5/"),
    CC_BY_SA_2_0("CC-BY-SA-2.0", "CC BY-SA 2.0", true, "https://creativecommons.org/licenses/by-sa/2.0/"),
    CC_BY_SA_1_0("CC-BY-SA-1.0", "CC BY-SA 1.0", true, "https://creativecommons.org/licenses/by-sa/1.0/"),
    PUBLIC_DOMAIN(
        "LicenseRef-PublicDomain",
        "Public domain",
        false,
        "https://creativecommons.org/publicdomain/mark/1.0/",
    ),
    ;

    companion object {
        /** Also accepts the enum name or the label ("CC BY-SA 3.0", "Public domain"), as in Commons exports. */
        fun fromSpdx(value: String): Licence =
            entries.find {
                it.spdx.equals(value, ignoreCase = true) ||
                    it.name.equals(value, ignoreCase = true) ||
                    it.label.equals(value, ignoreCase = true)
            }
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.INVALID_INPUT,
                    "Unknown licence '$value', expected one of ${entries.joinToString { it.spdx }}.",
                )
    }
}
