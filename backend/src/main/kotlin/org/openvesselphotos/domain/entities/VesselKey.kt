package org.openvesselphotos.domain.entities

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException

/** A CFR (3-letter country code + 9 characters) or an MMSI (9 digits), normalized. */
data class VesselKey(
    val type: KeyType,
    val value: String,
) {
    companion object {
        private val CFR_REGEX = Regex("^[A-Z]{3}[A-Z0-9]{9}$")
        private val MMSI_REGEX = Regex("^[0-9]{9}$")

        fun normalize(raw: String): String = raw.uppercase().replace(Regex("[\\s.-]"), "")

        fun of(
            type: KeyType,
            raw: String,
        ): VesselKey {
            val value = normalize(raw)
            val isValid =
                when (type) {
                    KeyType.CFR -> CFR_REGEX.matches(value)
                    KeyType.MMSI -> MMSI_REGEX.matches(value)
                }
            if (!isValid) {
                val expected =
                    if (type ==
                        KeyType.CFR
                    ) {
                        "3 letters + 9 characters, e.g. FRA000123456"
                    } else {
                        "9 digits, e.g. 227000101"
                    }
                throw BackendUsageException(
                    BackendUsageErrorCode.INVALID_KEY,
                    "Invalid ${type.name}: '$raw' ($expected).",
                )
            }

            return VesselKey(type, value)
        }

        /** Returns the key a search query looks like, if any. */
        fun detect(raw: String): VesselKey? {
            val value = normalize(raw)

            return when {
                MMSI_REGEX.matches(value) -> VesselKey(KeyType.MMSI, value)
                CFR_REGEX.matches(value) -> VesselKey(KeyType.CFR, value)
                else -> null
            }
        }
    }
}
