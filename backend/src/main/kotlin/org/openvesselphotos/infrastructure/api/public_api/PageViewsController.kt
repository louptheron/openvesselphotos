package org.openvesselphotos.infrastructure.api.public_api

import jakarta.servlet.http.HttpServletRequest
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.public_api.RecordUsage
import org.openvesselphotos.infrastructure.api.inputs.PageViewDataInput
import org.openvesselphotos.infrastructure.web.PublicUrl
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * The pages of both websites send their path when shown (client-side navigations included), counted by site for the
 * admin's statistics. Only the websites' own pages may send it: see ApiKeyCheckFilter.
 */
@RestController
class PageViewsController(
    private val recordUsage: RecordUsage,
    private val publicUrl: PublicUrl,
) {
    @PostMapping(PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun record(
        @RequestBody input: PageViewDataInput,
        request: HttpServletRequest,
    ) {
        recordUsage.recordPageView(publicUrl.siteOf(request.serverName).website, normalize(input.path))
    }

    companion object {
        const val PATH = "/v1/page-views"
        private const val MAX_PATH_LENGTH = 255

        /** The path without query nor fragment, nor trailing slash, e.g. "/vessels/cfr/FRA000123456" */
        fun normalize(path: String): String {
            val pagePath = path.trim().substringBefore('?').substringBefore('#')
            if (!pagePath.startsWith("/") || pagePath.startsWith("//") || pagePath.length > MAX_PATH_LENGTH) {
                throw BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, "Invalid page path.")
            }

            return pagePath.trimEnd('/').ifEmpty { "/" }
        }
    }
}
