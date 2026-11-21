package org.openvesselphotos.infrastructure.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI

/**
 * Base URL of each site in canonical links and sitemaps. OPENVESSELPHOTOS_PUBLIC_URL and
 * OPENVESSELPHOTOS_PORTS_PUBLIC_URL pin them (e.g. a custom domain rather than *.scalingo.io), so that search engines
 * index a single host per site. Each defaults to the requested host.
 */
@Component
class PublicUrl(
    @param:Value("\${openvesselphotos.public-url}")
    configuredUrl: String,
    @param:Value("\${openvesselphotos.ports-public-url}")
    portsConfiguredUrl: String,
) {
    private val urls =
        mapOf(
            Site.OPENVESSELPHOTOS to configuredUrl.trimEnd('/'),
            Site.PORTS to portsConfiguredUrl.trimEnd('/'),
        )
    private val portsHost = hostOf(urls.getValue(Site.PORTS))

    fun get(site: Site = Site.OPENVESSELPHOTOS): String =
        urls.getValue(site).ifEmpty {
            val requestedUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
            // In local development, Open Vessel Photos is on localhost, next to the ports site on ports.localhost
            if (site ==
                Site.OPENVESSELPHOTOS
            ) {
                requestedUrl.replace("//${Site.PORTS_LOCAL_HOST}", "//localhost")
            } else {
                requestedUrl
            }
        }

    /**
     * The URL of the ports site from Open Vessel Photos, to link to it: next to localhost (on ports.localhost) in
     * local development
     */
    fun portsSite(): String =
        urls.getValue(Site.PORTS).ifEmpty {
            get(Site.OPENVESSELPHOTOS).replace("//localhost", "//${Site.PORTS_LOCAL_HOST}")
        }

    /** The configured URL of the site, empty when not set */
    fun configured(site: Site): String = urls.getValue(site)

    /** The site served on this host, e.g. "ports-de-peche.fr" (or its www.) for the ports site */
    fun siteOf(host: String): Site {
        val requestedHost = host.lowercase().removePrefix("www.")

        return if (requestedHost == portsHost || requestedHost == Site.PORTS_LOCAL_HOST) {
            Site.PORTS
        } else {
            Site.OPENVESSELPHOTOS
        }
    }

    companion object {
        fun hostOf(url: String): String? =
            url
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { URI.create(it).host }.getOrNull() }
                ?.lowercase()
                ?.removePrefix("www.")
    }
}
