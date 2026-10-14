package org.openvesselphotos.domain.entities

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException

enum class KeyType(
    val pathName: String,
) {
    CFR("cfr"),
    MMSI("mmsi"),
    ;

    /** A vessel has at most one key of each type: a CFR and an MMSI. */
    val other: KeyType get() = if (this == CFR) MMSI else CFR

    companion object {
        fun fromPathName(value: String): KeyType =
            entries.find { it.pathName == value.lowercase() }
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.INVALID_KEY,
                    "Unknown key type '$value', expected cfr or mmsi.",
                )
    }
}
