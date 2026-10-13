package org.openvesselphotos.infrastructure.api

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException

data class ApiError(
    val code: BackendUsageErrorCode,
    val message: String,
)

@RestControllerAdvice
class ControllersExceptionHandler {
    @ExceptionHandler(BackendUsageException::class)
    fun handleBackendUsageException(e: BackendUsageException): ResponseEntity<ApiError> {
        val status =
            when (e.code) {
                BackendUsageErrorCode.NOT_FOUND -> HttpStatus.NOT_FOUND
                BackendUsageErrorCode.CONFLICT -> HttpStatus.CONFLICT
                BackendUsageErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED
                BackendUsageErrorCode.TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS
                BackendUsageErrorCode.INVALID_KEY,
                BackendUsageErrorCode.INVALID_INPUT,
                BackendUsageErrorCode.INVALID_IMAGE,
                -> HttpStatus.BAD_REQUEST
            }

        return ResponseEntity.status(status).body(ApiError(e.code, e.message))
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.CONTENT_TOO_LARGE)
            .body(ApiError(BackendUsageErrorCode.INVALID_IMAGE, "The file is larger than 15 MB."))

    @ExceptionHandler(MissingServletRequestParameterException::class, MissingServletRequestPartException::class)
    fun handleMissingParameter(e: Exception): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(
            ApiError(BackendUsageErrorCode.INVALID_INPUT, e.message ?: "Missing parameter."),
        )
}
