package org.openvesselphotos.infrastructure.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VesselsWithoutPhotosReleaseUTests {
    @Test
    fun `limit Should grow by the number per day from the start day, up to one sitemap file`() {
        val release = VesselsWithoutPhotosRelease("2026-10-27", 10)

        assertThat(release.limit(LocalDate.parse("2026-10-26"))).isEqualTo(0)
        assertThat(release.limit(LocalDate.parse("2026-10-27"))).isEqualTo(10)
        assertThat(release.limit(LocalDate.parse("2026-11-05"))).isEqualTo(100)
        assertThat(release.limit(LocalDate.parse("2036-01-01"))).isEqualTo(CrawlersController.VESSELS_PER_SITEMAP)
        assertThat(VesselsWithoutPhotosRelease("", 10).limit()).isEqualTo(0)
    }
}
