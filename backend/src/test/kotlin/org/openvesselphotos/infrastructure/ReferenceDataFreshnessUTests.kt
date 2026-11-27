package org.openvesselphotos.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.infrastructure.eu.CsvFishingOpportunityRepository
import org.openvesselphotos.infrastructure.ifremer.CsvStockAssessmentRepository
import java.time.LocalDate
import java.time.MonthDay

/**
 * Fails the build once a reference file copied by hand is a year behind, in case the weekly watch
 * (.github/workflows/reference-data.yml) missed the new edition or its issue was left open. See the README to refresh.
 */
class ReferenceDataFreshnessUTests {
    @Test
    fun `the EU TACs Should be those of the current year from March`() {
        // The regulation is adopted in December or January (2026/249 on 26 January 2026)
        val year = CsvFishingOpportunityRepository().find().year

        assertThat(year)
            .describedAs("eu/fishing-opportunities.csv is for $year: copy the TACs of the new regulation")
            .isGreaterThanOrEqualTo(latestExpected(LocalDate.now(), MonthDay.of(3, 1), delay = 0))
    }

    @Test
    fun `the Ifremer stock statuses Should be those assessed last year from June`() {
        // The Bilan of the stocks assessed in a year is published the next spring (2025's on 2 March 2026)
        val edition = CsvStockAssessmentRepository().find().edition

        assertThat(edition)
            .describedAs("ifremer/stock-status.csv is the Bilan $edition: copy the new one")
            .isGreaterThanOrEqualTo(latestExpected(LocalDate.now(), MonthDay.of(6, 1), delay = 1))
    }

    @Test
    fun `latestExpected Should wait until the deadline of the year`() {
        assertThat(latestExpected(LocalDate.of(2027, 2, 28), MonthDay.of(3, 1), delay = 0)).isEqualTo(2026)
        assertThat(latestExpected(LocalDate.of(2027, 3, 1), MonthDay.of(3, 1), delay = 0)).isEqualTo(2027)
        assertThat(latestExpected(LocalDate.of(2027, 5, 31), MonthDay.of(6, 1), delay = 1)).isEqualTo(2025)
        assertThat(latestExpected(LocalDate.of(2027, 6, 1), MonthDay.of(6, 1), delay = 1)).isEqualTo(2026)
    }

    /** The oldest edition still accepted [today]: the edition of the year, less [delay], is due by [deadline] */
    private fun latestExpected(
        today: LocalDate,
        deadline: MonthDay,
        delay: Int,
    ): Int {
        val year = if (MonthDay.from(today) < deadline) today.year - 1 else today.year
        return year - delay
    }
}
