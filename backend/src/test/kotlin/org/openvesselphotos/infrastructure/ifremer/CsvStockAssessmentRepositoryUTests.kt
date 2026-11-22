package org.openvesselphotos.infrastructure.ifremer

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.FishingRegion
import org.openvesselphotos.domain.entities.StockStatus

class CsvStockAssessmentRepositoryUTests {
    @Test
    fun `find Should read every row of the bundled assessment`() {
        // Any typo in a status or a region of the yearly update fails here
        val assessment = CsvStockAssessmentRepository().find()

        assertThat(assessment.edition).isEqualTo(2025)
        assertThat(assessment.url).startsWith("https://archimer.ifremer.fr/")
        assertThat(assessment.bySpecies).hasSizeGreaterThan(30)
        // Matched by the normalized FranceAgriMer name
        val sole = assessment.bySpecies.getValue("sole")
        assertThat(sole.inRegion(FishingRegion.MNMEST)?.status).isEqualTo(StockStatus.EFFONDRE)
        assertThat(sole.inRegion(FishingRegion.GASCOGNE)?.status).isEqualTo(StockStatus.RECONSTITUABLE)
        assertThat(assessment.bySpecies.getValue("coquillesaintjacques").status).isEqualTo(StockStatus.BON_ETAT)
    }

    @Test
    fun `parse Should reject an unknown status`() {
        val text =
            """
            # edition: 2025
            # url: https://example.org
            species;region;stock;status;tonnes
            Sole;MNMEST;Sole commune, Division VIId;MAUVAIS;10
            """.trimIndent()

        assertThatThrownBy {
            CsvStockAssessmentRepository.parse(
                text,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
