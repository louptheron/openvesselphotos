package org.openvesselphotos.infrastructure.api.admin

import jakarta.servlet.http.HttpServletRequest
import org.openvesselphotos.config.AdminAuthentication
import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.use_cases.admin.ApprovePhoto
import org.openvesselphotos.domain.use_cases.admin.GetPendingPhotoImage
import org.openvesselphotos.domain.use_cases.admin.GetReviewQueue
import org.openvesselphotos.domain.use_cases.admin.RejectPhoto
import org.openvesselphotos.domain.use_cases.admin.ResolveReport
import org.openvesselphotos.domain.use_cases.admin.ReviewKeyLink
import org.openvesselphotos.infrastructure.api.inputs.ApprovePhotoDataInput
import org.openvesselphotos.infrastructure.api.inputs.LoginDataInput
import org.openvesselphotos.infrastructure.api.inputs.RejectPhotoDataInput
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Every route but /login is protected by [org.openvesselphotos.config.AdminAuthenticationFilter]. */
@RestController
@RequestMapping("/v1/admin")
class AdminController(
    private val adminAuthentication: AdminAuthentication,
    private val getReviewQueue: GetReviewQueue,
    private val getPendingPhotoImage: GetPendingPhotoImage,
    private val approvePhoto: ApprovePhoto,
    private val rejectPhoto: RejectPhoto,
    private val reviewKeyLink: ReviewKeyLink,
    private val resolveReport: ResolveReport,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody input: LoginDataInput,
        request: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val cookie = adminAuthentication.login(input.password, request.remoteAddr, request.isSecure)

        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build()
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Unit> =
        ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, adminAuthentication.logoutCookie(request.isSecure).toString())
            .build()

    @GetMapping("/queue")
    fun queue(): ReviewQueueDataOutput = ReviewQueueDataOutput.from(getReviewQueue.execute())

    @GetMapping("/photos/{id}.jpg", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun photoImage(
        @PathVariable id: Int,
        @RequestParam(required = false) size: String?,
    ): ResponseEntity<ByteArray> =
        ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.noStore())
            .body(getPendingPhotoImage.execute(id, ImageSize.fromParam(size)))

    @PostMapping("/photos/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun approve(
        @PathVariable id: Int,
        @RequestBody(required = false) input: ApprovePhotoDataInput?,
    ) {
        approvePhoto.execute(id, input?.vesselName)
    }

    @PostMapping("/photos/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reject(
        @PathVariable id: Int,
        @RequestBody(required = false) input: RejectPhotoDataInput?,
    ) {
        rejectPhoto.execute(id, input?.reason)
    }

    @PostMapping("/links/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun approveLink(
        @PathVariable id: Int,
    ) {
        reviewKeyLink.execute(id, approve = true)
    }

    @PostMapping("/links/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rejectLink(
        @PathVariable id: Int,
    ) {
        reviewKeyLink.execute(id, approve = false)
    }

    @PostMapping("/reports/{id}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun dismissReport(
        @PathVariable id: Int,
    ) {
        resolveReport.execute(id, removePhoto = false)
    }

    @PostMapping("/reports/{id}/remove-photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeReportedPhoto(
        @PathVariable id: Int,
    ) {
        resolveReport.execute(id, removePhoto = true)
    }
}
