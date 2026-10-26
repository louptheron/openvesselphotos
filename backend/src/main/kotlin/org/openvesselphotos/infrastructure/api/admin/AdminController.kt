package org.openvesselphotos.infrastructure.api.admin

import jakarta.servlet.http.HttpServletRequest
import org.openvesselphotos.config.AdminAuthentication
import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.use_cases.admin.ApproveApiKeyRequest
import org.openvesselphotos.domain.use_cases.admin.ApprovePhoto
import org.openvesselphotos.domain.use_cases.admin.GetActiveApiKeys
import org.openvesselphotos.domain.use_cases.admin.GetPendingPhotoImage
import org.openvesselphotos.domain.use_cases.admin.GetReviewQueue
import org.openvesselphotos.domain.use_cases.admin.GetUsageStatistics
import org.openvesselphotos.domain.use_cases.admin.ImportPhotos
import org.openvesselphotos.domain.use_cases.admin.ImportVessels
import org.openvesselphotos.domain.use_cases.admin.ImportWikidata
import org.openvesselphotos.domain.use_cases.admin.PhotoImportStatus
import org.openvesselphotos.domain.use_cases.admin.PhotoNotesResult
import org.openvesselphotos.domain.use_cases.admin.RejectApiKeyRequest
import org.openvesselphotos.domain.use_cases.admin.RejectPhoto
import org.openvesselphotos.domain.use_cases.admin.ResolveReport
import org.openvesselphotos.domain.use_cases.admin.ReviewKeyLink
import org.openvesselphotos.domain.use_cases.admin.RevokeApiKey
import org.openvesselphotos.domain.use_cases.admin.SetPhotoNotes
import org.openvesselphotos.domain.use_cases.admin.UsageStatistics
import org.openvesselphotos.domain.use_cases.admin.VesselImportResult
import org.openvesselphotos.domain.use_cases.admin.WikidataImportStatus
import org.openvesselphotos.infrastructure.api.inputs.ApprovePhotoDataInput
import org.openvesselphotos.infrastructure.api.inputs.LoginDataInput
import org.openvesselphotos.infrastructure.api.inputs.RejectPhotoDataInput
import org.openvesselphotos.infrastructure.api.security.ClientIp
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** Every route but /login is protected by [org.openvesselphotos.config.AdminAuthenticationFilter]. */
@RestController
@RequestMapping("/v1/admin")
class AdminController(
    private val adminAuthentication: AdminAuthentication,
    private val clientIp: ClientIp,
    private val getReviewQueue: GetReviewQueue,
    private val getPendingPhotoImage: GetPendingPhotoImage,
    private val approvePhoto: ApprovePhoto,
    private val rejectPhoto: RejectPhoto,
    private val reviewKeyLink: ReviewKeyLink,
    private val resolveReport: ResolveReport,
    private val getActiveApiKeys: GetActiveApiKeys,
    private val approveApiKeyRequest: ApproveApiKeyRequest,
    private val rejectApiKeyRequest: RejectApiKeyRequest,
    private val revokeApiKey: RevokeApiKey,
    private val importVessels: ImportVessels,
    private val importPhotos: ImportPhotos,
    private val importWikidata: ImportWikidata,
    private val setPhotoNotes: SetPhotoNotes,
    private val getUsageStatistics: GetUsageStatistics,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody input: LoginDataInput,
        request: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val cookie = adminAuthentication.login(input.password, clientIp.of(request), request.isSecure)

        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build()
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Unit> =
        ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, adminAuthentication.logoutCookie(request.isSecure).toString())
            .build()

    /** Usage of the photos and of the API over the last [days] days (30 by default, 365 at most). */
    @GetMapping("/usage")
    fun usage(
        @RequestParam(required = false, defaultValue = "30") days: Int,
    ): UsageStatistics = getUsageStatistics.execute(days)

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

    @GetMapping("/api-keys")
    fun activeApiKeys(): List<ApiKeyDataOutput> = getActiveApiKeys.execute().map(ApiKeyDataOutput::from)

    /** The only time the key is shown: it is stored as a hash. */
    @PostMapping("/api-keys/{id}/approve")
    fun approveApiKey(
        @PathVariable id: Int,
    ): ApprovedApiKeyDataOutput {
        val approved = approveApiKeyRequest.execute(id)

        return ApprovedApiKeyDataOutput(key = approved.key, apiKey = ApiKeyDataOutput.from(approved.apiKey))
    }

    @PostMapping("/api-keys/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rejectApiKey(
        @PathVariable id: Int,
    ) {
        rejectApiKeyRequest.execute(id)
    }

    @PostMapping("/api-keys/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeApiKeyById(
        @PathVariable id: Int,
    ) {
        revokeApiKey.execute(id)
    }

    /** Creates vessels without photos from a CSV file, e.g. an export of the EU fleet register. */
    @PostMapping("/vessels/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importVesselsFromCsv(
        @RequestPart file: MultipartFile,
    ): VesselImportResult = importVessels.execute(VesselsCsvParser.parse(String(file.bytes, Charsets.UTF_8)))

    /**
     * Completes the vessels with photos with what Wikidata knows of them, found by their MMSI: starts a background
     * import and returns its status at once.
     */
    @PostMapping("/vessels/wikidata")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun importWikidata(): WikidataImportStatus = importWikidata.start()

    @GetMapping("/vessels/wikidata")
    fun wikidataImportStatus(): WikidataImportStatus = importWikidata.status()

    /**
     * Publishes openly licensed photos without review, e.g. from Wikimedia Commons: starts a background import and
     * returns its status at once. The images are downloaded one at a time.
     */
    @PostMapping("/photos/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun importPhotosFromCsv(
        @RequestPart file: MultipartFile,
    ): PhotoImportStatus = importPhotos.start(PhotosCsvParser.parse(String(file.bytes, Charsets.UTF_8)))

    /** Sets the notes shown with published photos, e.g. "Photographed under its former name LE CRI DES FLOTS". */
    @PostMapping("/photos/notes", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun setPhotoNotesFromCsv(
        @RequestPart file: MultipartFile,
    ): PhotoNotesResult = setPhotoNotes.execute(PhotosCsvParser.parseNotes(String(file.bytes, Charsets.UTF_8)))

    @GetMapping("/photos/import")
    fun photoImportStatus(): PhotoImportStatus = importPhotos.status()

    @PostMapping("/photos/import/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancelPhotoImport() {
        importPhotos.cancel()
    }
}
