package org.openvesselphotos.domain.entities

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException

enum class ImageSize(
    val param: String,
    /** Target width for resized images, max long edge for FULL. */
    val pixels: Int,
) {
    SMALL("320", 320),
    MEDIUM("800", 800),
    FULL("full", 2400),
    ;

    companion object {
        val DEFAULT = MEDIUM

        fun fromParam(value: String?): ImageSize =
            if (value.isNullOrBlank()) {
                DEFAULT
            } else {
                entries.find { it.param == value.lowercase() }
                    ?: throw BackendUsageException(
                        BackendUsageErrorCode.INVALID_INPUT,
                        "Unknown size '$value', expected 320, 800 or full.",
                    )
            }
    }
}
