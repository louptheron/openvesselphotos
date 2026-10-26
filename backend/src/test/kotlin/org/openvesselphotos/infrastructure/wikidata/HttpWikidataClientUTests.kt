package org.openvesselphotos.infrastructure.wikidata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.WikidataData
import org.openvesselphotos.infrastructure.wikidata.HttpWikidataClient.SparqlResponse
import org.openvesselphotos.infrastructure.wikidata.HttpWikidataClient.SparqlResults
import org.openvesselphotos.infrastructure.wikidata.HttpWikidataClient.SparqlValue

class HttpWikidataClientUTests {
    private fun row(vararg values: Pair<String, String>) =
        values.associate { (name, value) ->
            name to
                SparqlValue(value)
        }

    @Test
    fun `query Should only ask for valid MMSIs`() {
        val query = HttpWikidataClient.query(listOf("366904950", "\"} DROP", "12345"))

        assertThat(query).contains("""VALUES ?mmsi { "366904950" }""").doesNotContain("DROP")
    }

    @Test
    fun `parse Should read each vessel, and leave out invalid values and MMSIs found on two items`() {
        val response =
            SparqlResponse(
                SparqlResults(
                    listOf(
                        row(
                            "mmsi" to "366904950",
                            "item" to "http://www.wikidata.org/entity/Q7868346",
                            "imo" to "5385625",
                            "typeEn" to "replenishment oiler",
                            "typeFr" to "pétrolier ravitailleur",
                            "yearInService" to "1942",
                            "length" to "252",
                            "names" to "USS Chiwawa|USS CHIWAWA|Lee A. Tregurtha",
                        ),
                        row(
                            "mmsi" to "235054747",
                            "item" to "http://www.wikidata.org/entity/Q108196210",
                            "imo" to "not an IMO",
                            "yearBuilt" to "2003",
                            "yearInService" to "2004",
                            "length" to "-1",
                        ),
                        row("mmsi" to "111111111", "item" to "http://www.wikidata.org/entity/Q1"),
                        row("mmsi" to "111111111", "item" to "http://www.wikidata.org/entity/Q2"),
                    ),
                ),
            )

        assertThat(HttpWikidataClient.parse(response)).isEqualTo(
            mapOf(
                "366904950" to
                    WikidataData(
                        id = "Q7868346",
                        imo = "5385625",
                        shipTypeEn = "replenishment oiler",
                        shipTypeFr = "pétrolier ravitailleur",
                        yearBuilt = 1942,
                        lengthMeters = 252.0,
                        otherNames = listOf("USS Chiwawa", "Lee A. Tregurtha"),
                    ),
                "235054747" to WikidataData(id = "Q108196210", yearBuilt = 2003),
            ),
        )
    }
}
