package org.openvesselphotos.infrastructure.api.public_api

import org.openvesselphotos.domain.entities.ImageSize
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Licence
import org.openvesselphotos.domain.entities.ReportReason
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.public_api.ExportDataset
import org.openvesselphotos.domain.use_cases.public_api.GetPhotoImage
import org.openvesselphotos.domain.use_cases.public_api.GetRecentVessels
import org.openvesselphotos.domain.use_cases.public_api.GetVessel
import org.openvesselphotos.domain.use_cases.public_api.PhotoSubmission
import org.openvesselphotos.domain.use_cases.public_api.ReportPhoto
import org.openvesselphotos.domain.use_cases.public_api.RequestApiKey
import org.openvesselphotos.domain.use_cases.public_api.SearchVessels
import org.openvesselphotos.domain.use_cases.public_api.SubmitPhoto
import org.openvesselphotos.domain.use_cases.public_api.SuggestKeyLink
import org.openvesselphotos.infrastructure.api.inputs.ApiKeyRequestDataInput
import org.openvesselphotos.infrastructure.api.inputs.KeyLinkDataInput
import org.openvesselphotos.infrastructure.api.inputs.ReportDataInput
import org.openvesselphotos.infrastructure.api.outputs.ExportedVesselDataOutput
import org.openvesselphotos.infrastructure.api.outputs.PhotoDataOutput
import org.openvesselphotos.infrastructure.api.outputs.SubmissionDataOutput
import org.openvesselphotos.infrastructure.api.outputs.VesselDataOutput
import org.openvesselphotos.infrastructure.api.outputs.VesselSummaryDataOutput
import org.openvesselphotos.infrastructure.api.security.ApiKeyCheckFilter
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/v1")
class PublicVesselController(
    private val searchVessels: SearchVessels,
    private val getRecentVessels: GetRecentVessels,
    private val getVessel: GetVessel,
    private val getPhotoImage: GetPhotoImage,
    private val submitPhoto: SubmitPhoto,
    private val suggestKeyLink: SuggestKeyLink,
    private val reportPhoto: ReportPhoto,
    private val exportDataset: ExportDataset,
    private val requestApiKey: RequestApiKey,
) {
    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
    ): List<VesselSummaryDataOutput> = searchVessels.execute(q).map { VesselSummaryDataOutput.from(baseUrl(), it) }

    @GetMapping("/vessels/recent")
    fun recent(
        @RequestParam(defaultValue = "12") limit: Int,
    ): List<VesselSummaryDataOutput> =
        getRecentVessels.execute(limit).map { VesselSummaryDataOutput.from(baseUrl(), it) }

    @GetMapping("/export.json")
    fun export(): List<ExportedVesselDataOutput> {
        val baseUrl = baseUrl()

        return exportDataset.execute().map { (vessel, photos) ->
            ExportedVesselDataOutput(
                vessel.cfr,
                vessel.mmsi,
                vessel.name,
                photos.map { PhotoDataOutput.from(baseUrl, vessel, it) },
            )
        }
    }

    @GetMapping("/{keyType:cfr|mmsi}/{key:[A-Za-z0-9]+}")
    fun vessel(
        @PathVariable keyType: String,
        @PathVariable key: String,
    ): VesselDataOutput = VesselDataOutput.from(baseUrl(), getVessel.execute(toKey(keyType, key)))

    @GetMapping("/{keyType:cfr|mmsi}/{key:[A-Za-z0-9]+}.jpg", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun mainPhoto(
        @PathVariable keyType: String,
        @PathVariable key: String,
        @RequestParam(required = false) size: String?,
    ): ResponseEntity<ByteArray> = image(keyType, key, null, size)

    @GetMapping("/{keyType:cfr|mmsi}/{key:[A-Za-z0-9]+}/photos/{n:[0-9]+}.jpg", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun photo(
        @PathVariable keyType: String,
        @PathVariable key: String,
        @PathVariable n: Int,
        @RequestParam(required = false) size: String?,
    ): ResponseEntity<ByteArray> = image(keyType, key, n, size)

    @PostMapping("/{keyType:cfr|mmsi}/{key:[A-Za-z0-9]+}/photos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun submit(
        @PathVariable keyType: String,
        @PathVariable key: String,
        @RequestPart file: MultipartFile,
        @RequestParam credit: String,
        @RequestParam licence: String,
        @RequestParam(required = false) takenOn: String?,
        @RequestParam(required = false) place: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) vesselName: String?,
        @RequestAttribute(name = ApiKeyCheckFilter.API_KEY_ID_ATTRIBUTE, required = false) apiKeyId: Int?,
    ): SubmissionDataOutput {
        val photo =
            submitPhoto.execute(
                PhotoSubmission(
                    key = toKey(keyType, key),
                    image = file.bytes,
                    credit = credit,
                    licence = Licence.fromSpdx(licence),
                    takenOn = parseDate(takenOn),
                    place = place,
                    contributorEmail = email,
                    vesselName = vesselName,
                    apiKeyId = apiKeyId,
                ),
            )

        return SubmissionDataOutput(requireNotNull(photo.id), photo.status.name)
    }

    /** Open to everyone: it is how one gets an API key. Keys are given by an admin after review. */
    @PostMapping("/api-key-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun requestApiKey(
        @RequestBody input: ApiKeyRequestDataInput,
    ): SubmissionDataOutput {
        val request = requestApiKey.execute(input.name, input.email, input.organisation, input.intendedUse)

        return SubmissionDataOutput(requireNotNull(request.id), request.status.name)
    }

    @PostMapping("/{keyType:cfr|mmsi}/{key:[A-Za-z0-9]+}/link")
    @ResponseStatus(HttpStatus.CREATED)
    fun link(
        @PathVariable keyType: String,
        @PathVariable key: String,
        @RequestBody input: KeyLinkDataInput,
    ): SubmissionDataOutput {
        val request =
            suggestKeyLink.execute(
                toKey(keyType, key),
                VesselKey.of(KeyType.fromPathName(input.keyType), input.key),
                input.contributor,
            )

        return SubmissionDataOutput(requireNotNull(request.id), request.status.name)
    }

    @PostMapping("/{keyType:cfr|mmsi}/{key:[A-Za-z0-9]+}/photos/{n:[0-9]+}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun report(
        @PathVariable keyType: String,
        @PathVariable key: String,
        @PathVariable n: Int,
        @RequestBody input: ReportDataInput,
    ): SubmissionDataOutput {
        val reason =
            ReportReason.entries.find { it.name.equals(input.reason, ignoreCase = true) }
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.INVALID_INPUT,
                    "Unknown reason '${input.reason}', expected one of ${ReportReason.entries.joinToString()}.",
                )
        val report = reportPhoto.execute(toKey(keyType, key), n, reason, input.message)

        return SubmissionDataOutput(requireNotNull(report.id), report.status.name)
    }

    private fun image(
        keyType: String,
        key: String,
        number: Int?,
        size: String?,
    ): ResponseEntity<ByteArray> {
        val image = getPhotoImage.execute(toKey(keyType, key), number, ImageSize.fromParam(size))

        return ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
            .body(image.content)
    }

    private fun toKey(
        keyType: String,
        key: String,
    ) = VesselKey.of(KeyType.fromPathName(keyType), key)

    private fun parseDate(value: String?): LocalDate? =
        try {
            value?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        } catch (e: DateTimeParseException) {
            throw BackendUsageException(
                BackendUsageErrorCode.INVALID_INPUT,
                "Invalid date '$value', expected YYYY-MM-DD.",
            )
        }

    private fun baseUrl() = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
}
