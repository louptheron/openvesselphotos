package org.openvesselphotos.infrastructure.api.public_api

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.public_api.GetUnseenSubmissions
import org.openvesselphotos.domain.use_cases.public_api.MarkSubmissionSeen
import org.openvesselphotos.infrastructure.api.outputs.ReviewedSubmissionDataOutput
import org.openvesselphotos.infrastructure.api.security.ApiKeyCheckFilter
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/** The reviews of the photos sent with an API key, for its application to tell its contributors. */
@RestController
@RequestMapping("/v1/submissions")
class SubmissionsController(
    private val getUnseenSubmissions: GetUnseenSubmissions,
    private val markSubmissionSeen: MarkSubmissionSeen,
) {
    @GetMapping
    fun unseen(
        @RequestParam contributorId: String,
        @RequestAttribute(name = ApiKeyCheckFilter.API_KEY_ID_ATTRIBUTE, required = false) apiKeyId: Int?,
    ): List<ReviewedSubmissionDataOutput> {
        val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()

        return getUnseenSubmissions
            .execute(requireApiKey(apiKeyId), contributorId)
            .map { ReviewedSubmissionDataOutput.from(baseUrl, it) }
    }

    @PostMapping("/{id:[0-9]+}/seen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun seen(
        @PathVariable id: Int,
        @RequestAttribute(name = ApiKeyCheckFilter.API_KEY_ID_ATTRIBUTE, required = false) apiKeyId: Int?,
    ) {
        markSubmissionSeen.execute(requireApiKey(apiKeyId), id)
    }

    /** A valid key is needed even for the GET: the submissions are those of the key. */
    private fun requireApiKey(apiKeyId: Int?): Int =
        apiKeyId ?: throw BackendUsageException(
            BackendUsageErrorCode.UNAUTHORIZED,
            "A valid API key is required (x-api-key header).",
        )
}
