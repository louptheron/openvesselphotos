package org.openvesselphotos.infrastructure.api.inputs

data class KeyLinkDataInput(
    val keyType: String,
    val key: String,
    val contributor: String? = null,
)

data class ReportDataInput(
    val reason: String,
    val message: String? = null,
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
