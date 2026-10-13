package org.openvesselphotos.infrastructure.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Base URL of the site in canonical links and the sitemap. OPENVESSELPHOTOS_PUBLIC_URL pins it (e.g. a custom domain
 * rather than *.scalingo.io), so that search engines index a single host. Defaults to the requested host.
 */
@Component
class PublicUrl(
    @param:Value("\${openvesselphotos.public-url}")
    private val configuredUrl: String,
) {
    fun get(): String =
        configuredUrl.trimEnd('/').ifEmpty {
            ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
        }
}
