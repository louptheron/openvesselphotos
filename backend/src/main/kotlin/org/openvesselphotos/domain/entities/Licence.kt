package org.openvesselphotos.domain.entities

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException

enum class Licence(
    /** SPDX identifier, used in the API. */
    val spdx: String,
    val label: String,
    val requiresCredit: Boolean,
) {
    CC0("CC0-1.0", "CC0", false),
    CC_BY_4_0("CC-BY-4.0", "CC BY 4.0", true),
    CC_BY_SA_4_0("CC-BY-SA-4.0", "CC BY-SA 4.0", true),
    ;

    companion object {
        fun fromSpdx(value: String): Licence =
            entries.find { it.spdx.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.INVALID_INPUT,
                    "Unknown licence '$value', expected one of ${entries.joinToString { it.spdx }}.",
                )
    }
}
