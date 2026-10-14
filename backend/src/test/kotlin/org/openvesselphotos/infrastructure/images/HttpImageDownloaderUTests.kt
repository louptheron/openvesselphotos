package org.openvesselphotos.infrastructure.images

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.exceptions.BackendUsageException

class HttpImageDownloaderUTests {
    @Test
    fun `download Should refuse other hosts than Wikimedia Commons and Flickr`() {
        val downloader = HttpImageDownloader()

        listOf(
            "https://169.254.169.254/latest/meta-data",
            "https://upload.wikimedia.org.example.com/a.jpg",
            "https://notwikimedia.org/a.jpg",
            "http://upload.wikimedia.org/a.jpg",
            "https://localhost/a.jpg",
        ).forEach { url ->
            assertThatThrownBy { downloader.download(url) }.isInstanceOf(BackendUsageException::class.java)
        }
    }
}
