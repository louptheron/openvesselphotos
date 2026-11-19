package org.openvesselphotos.infrastructure.web

import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.search_engines.SearchEngineNotifier
import org.openvesselphotos.infrastructure.images.HttpImageDownloader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Tells the search engines of the IndexNow protocol (Bing, Yandex, Seznam, Naver…; not Google) which vessel pages
 * changed. The URLs are sent by batch every minute: an import publishes hundreds of photos. Enabled when
 * OPENVESSELPHOTOS_INDEXNOW_KEY and OPENVESSELPHOTOS_PUBLIC_URL are both set: the key proves that the site sends them,
 * served at [KEY_PATH] (see [CrawlersController]).
 */
@Component
class IndexNowNotifier(
    @param:Value("\${openvesselphotos.indexnow-key}")
    configuredKey: String,
    @param:Value("\${openvesselphotos.public-url}")
    configuredUrl: String,
    private val jsonMapper: JsonMapper,
) : SearchEngineNotifier {
    private val logger = LoggerFactory.getLogger(IndexNowNotifier::class.java)
    private val baseUrl = configuredUrl.trimEnd('/')

    /** Null when disabled */
    val key: String? =
        configuredKey.trim().ifEmpty { null }?.takeIf {
            val isValid = KEY.matches(it)
            if (!isValid) {
                logger.warn("IndexNow disabled: its key must be 8 to 128 letters, digits or dashes")
            }
            isValid && baseUrl.isNotEmpty()
        }

    private val pendingUrls: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()

    override fun vesselChanged(vessel: Vessel) {
        if (key == null) {
            return
        }
        Language.entries.forEach { pendingUrls.add(baseUrl + it.path(PagesController.vesselPath(vessel))) }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    fun flush() {
        val key = key ?: return
        val urls = pendingUrls.take(MAX_URLS_PER_REQUEST).takeIf { it.isNotEmpty() } ?: return
        pendingUrls.removeAll(urls.toSet())
        val request =
            HttpRequest
                .newBuilder(URI(ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", HttpImageDownloader.USER_AGENT)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(key, urls)))
                .build()

        // Never retried: search engines crawl the sitemap anyway
        try {
            val status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
            if (status in 200..299) {
                logger.info("IndexNow: ${urls.size} URLs sent")
            } else {
                logger.warn("IndexNow answered HTTP $status for ${urls.size} URLs")
            }
        } catch (e: Exception) {
            logger.warn("IndexNow unreachable, ${urls.size} URLs not sent: ${e.message}")
        }
    }

    fun requestBody(
        key: String,
        urls: List<String>,
    ): String =
        jsonMapper.writeValueAsString(
            mapOf(
                "host" to URI(baseUrl).host,
                "key" to key,
                "keyLocation" to baseUrl + KEY_PATH,
                "urlList" to urls,
            ),
        )

    companion object {
        const val KEY_PATH = "/indexnow-key.txt"
        private const val ENDPOINT = "https://api.indexnow.org/indexnow"
        private const val MAX_URLS_PER_REQUEST = 10_000
        private val KEY = Regex("[a-zA-Z0-9-]{8,128}")
    }
}
