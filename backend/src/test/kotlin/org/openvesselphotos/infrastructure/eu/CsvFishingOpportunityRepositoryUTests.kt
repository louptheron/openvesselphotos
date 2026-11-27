package org.openvesselphotos.infrastructure.eu

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.FishingRegion
import org.openvesselphotos.domain.use_cases.admin.ImportAuctionSales
import org.openvesselphotos.infrastructure.ifremer.CsvStockAssessmentRepository

class CsvFishingOpportunityRepositoryUTests {
    @Test
    fun `find Should read every row of the bundled regulation`() {
        // Any typo in a region or a number of the yearly update fails here
        val opportunities = CsvFishingOpportunityRepository().find()

        assertThat(opportunities.year).isEqualTo(2026)
        assertThat(opportunities.url).startsWith("https://eur-lex.europa.eu/")
        val sole = opportunities.bySpecies.getValue("sole")
        assertThat(
            sole
                .inRegion(FishingRegion.MNMEST)
                ?.quotas
                ?.first()
                ?.tacCode,
        ).isEqualTo("SOL/07D.")
        assertThat(
            sole
                .inRegion(FishingRegion.GASCOGNE)
                ?.quotas
                ?.single()
                ?.franceTonnes,
        ).isEqualTo(2195.0)
        // A TAC in two regions counts once: Bay of Biscay, Channel and Celtic Sea
        assertThat(sole.franceTonnes).isEqualTo(669.0 + 180 + 411 + 57 + 28 + 4 + 2195)
        // Not set by the Union in Norwegian waters
        assertThat(
            opportunities.bySpecies
                .getValue("lieunoir")
                .inRegion(FishingRegion.BARENTS)
                ?.quotas
                ?.single()
                ?.tacTonnes,
        ).isNull()
    }

    @Test
    fun `find Should only have species and regions Ifremer assesses`() {
        // A species misspelled would silently show no quota
        val stocks = CsvStockAssessmentRepository().find().bySpecies
        val opportunities = CsvFishingOpportunityRepository().find().bySpecies

        opportunities.forEach { (species, quotas) ->
            assertThat(stocks).containsKey(species)
            quotas.quotas.forEach { assertThat(stocks.getValue(species).inRegion(it.region)).isNotNull() }
        }
        assertThat(opportunities.keys).contains(ImportAuctionSales.normalize("Raie lisse (blonde)"))
    }

    @Test
    fun `parse Should reject an unknown region or a missing cell`() {
        val header = "# year: 2026\n# url: https://example.org\nspecies;region;tac_code;area;tac_tonnes;france_tonnes\n"

        assertThatThrownBy {
            CsvFishingOpportunityRepository.parse(header + "Sole;MANCHE;SOL/07D.;Manche Est;1275;669")
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy {
            CsvFishingOpportunityRepository.parse(header + "Sole;MNMEST;SOL/07D.;1275;669")
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
