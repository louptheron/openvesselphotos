package org.openvesselphotos.infrastructure.api.inputs

data class KeyLinkDataInput(
    val keyType: String,
    val key: String,
    val contributor: String? = null,
)

data class HomePortCorrectionDataInput(
    /** UN/LOCODE of the port the vessel is based at, e.g. FRQUY */
    val locode: String,
    val comment: String? = null,
    val contributor: String? = null,
)

data class ReportDataInput(
    val reason: String,
    val message: String? = null,
)

data class ApiKeyRequestDataInput(
    val name: String,
    val email: String,
    val organisation: String? = null,
    val intendedUse: String,
)

data class LoginDataInput(
    val password: String,
)

data class ApprovePhotoDataInput(
    val vesselName: String? = null,
)

data class RejectPhotoDataInput(
    val reason: String? = null,
)

data class PageViewDataInput(
    /** The path of the page shown, e.g. "/vessels/cfr/FRA000123456" */
    val path: String,
)
