package org.openvesselphotos.infrastructure.images

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.images.ImageDownloader
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Downloads images from the hosts of Wikimedia Commons and Flickr only, so that an import file cannot make the
 * server fetch other addresses. Redirects are not followed, for the same reason.
 */
@Component
class HttpImageDownloader : ImageDownloader {
    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    override fun download(url: String): ByteArray {
        val uri = URI(url)
        val host = uri.host?.lowercase()
        if (uri.scheme != "https" || host == null || ALLOWED_HOSTS.none { host == it || host.endsWith(".$it") }) {
            throw BackendUsageException(
                BackendUsageErrorCode.INVALID_INPUT,
                "Images can only be imported from ${ALLOWED_HOSTS.joinToString()} (HTTPS).",
            )
        }
        val request =
            HttpRequest
                .newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build()

        repeat(MAX_ATTEMPTS) { attempt ->
            val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
            when (response.statusCode()) {
                200 -> return response.body().use { body -> readAtMost(body, MAX_BYTES) }
                // Too many requests: waits as asked, or longer at each attempt
                429, 503 -> {
                    response.body().close()
                    val retryAfter =
                        response
                            .headers()
                            .firstValue("Retry-After")
                            .orElse(null)
                            ?.toLongOrNull()
                    Thread.sleep((retryAfter ?: (30L * (attempt + 1))).coerceAtMost(300) * 1000)
                }
                else -> {
                    response.body().close()
                    throw IllegalStateException("HTTP ${response.statusCode()} for $url")
                }
            }
        }
        throw IllegalStateException("Still rate limited after $MAX_ATTEMPTS attempts: $url")
    }

    private fun readAtMost(
        input: java.io.InputStream,
        maxBytes: Int,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) {
                return output.toByteArray()
            }
            if (output.size() + read > maxBytes) {
                throw IllegalStateException("The image is larger than ${maxBytes / 1024 / 1024} MB.")
            }
            output.write(buffer, 0, read)
        }
    }

    companion object {
        val ALLOWED_HOSTS = listOf("wikimedia.org", "staticflickr.com")
        const val MAX_BYTES = 15 * 1024 * 1024
        const val MAX_ATTEMPTS = 4

        /** Wikimedia asks for a User-Agent naming the tool and how to reach its author */
        const val USER_AGENT = "OpenVesselPhotos/1.0 (https://github.com/louptheron/openvesselphotos)"
    }
}
