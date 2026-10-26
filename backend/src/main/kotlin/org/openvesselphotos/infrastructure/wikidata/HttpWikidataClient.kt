package org.openvesselphotos.infrastructure.wikidata

import org.openvesselphotos.domain.entities.WikidataData
import org.openvesselphotos.domain.wikidata.WikidataClient
import org.openvesselphotos.infrastructure.images.HttpImageDownloader
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Asks the Wikidata Query Service for vessels by their MMSI (property P587). */
@Component
class HttpWikidataClient(
    private val jsonMapper: JsonMapper,
) : WikidataClient {
    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build()

    override fun findByMmsi(mmsis: Collection<String>): Map<String, WikidataData> {
        if (mmsis.isEmpty()) {
            return emptyMap()
        }
        val request =
            HttpRequest
                .newBuilder(URI(ENDPOINT))
                .timeout(Duration.ofSeconds(90))
                .header("User-Agent", HttpImageDownloader.USER_AGENT)
                .header("Accept", "application/sparql-results+json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("query=${URLEncoder.encode(query(mmsis), Charsets.UTF_8)}"))
                .build()

        repeat(MAX_ATTEMPTS) { attempt ->
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            when (response.statusCode()) {
                200 -> return parse(jsonMapper.readValue(response.body(), SparqlResponse::class.java))
                // Too many requests: waits as asked, or longer at each attempt
                429, 503 -> {
                    val retryAfter =
                        response
                            .headers()
                            .firstValue("Retry-After")
                            .orElse(null)
                            ?.toLongOrNull()
                    Thread.sleep((retryAfter ?: (30L * (attempt + 1))).coerceAtMost(300) * 1000)
                }
                else -> throw IllegalStateException("Wikidata answered HTTP ${response.statusCode()}")
            }
        }
        throw IllegalStateException("Wikidata still rate limited after $MAX_ATTEMPTS attempts")
    }

    data class SparqlResponse(
        val results: SparqlResults,
    )

    data class SparqlResults(
        val bindings: List<Map<String, SparqlValue>>,
    )

    data class SparqlValue(
        val value: String,
    )

    companion object {
        private const val ENDPOINT = "https://query.wikidata.org/sparql"
        private const val MAX_ATTEMPTS = 4
        private val MMSI = Regex("[0-9]{9}")
        private val IMO = Regex("[0-9]{7}")
        private val ITEM_ID = Regex("Q[0-9]+$")

        /**
         * One row per vessel: its IMO number, type (P31) in English and French, year of construction (P571) or entry
         * into service (P729), length in metres (P2043, normalized) and names (labels, P2561 name, P1448 official
         * name).
         */
        fun query(mmsis: Collection<String>): String =
            """
            SELECT ?mmsi ?item (SAMPLE(?imoValue) AS ?imo)
              (SAMPLE(?typeEnValue) AS ?typeEn) (SAMPLE(?typeFrValue) AS ?typeFr)
              (MIN(YEAR(?built)) AS ?yearBuilt) (MIN(YEAR(?inService)) AS ?yearInService)
              (MAX(?lengthValue) AS ?length)
              (GROUP_CONCAT(DISTINCT ?name; separator="|") AS ?names)
            WHERE {
              VALUES ?mmsi { ${mmsis.filter { MMSI.matches(it) }.joinToString(" ") { "\"$it\"" }} }
              ?item wdt:P587 ?mmsi .
              OPTIONAL { ?item wdt:P458 ?imoValue }
              OPTIONAL { ?item wdt:P31 ?type .
                OPTIONAL { ?type rdfs:label ?typeEnValue FILTER(LANG(?typeEnValue) = "en") }
                OPTIONAL { ?type rdfs:label ?typeFrValue FILTER(LANG(?typeFrValue) = "fr") } }
              OPTIONAL { ?item wdt:P571 ?built }
              OPTIONAL { ?item wdt:P729 ?inService }
              OPTIONAL { ?item p:P2043/psn:P2043/wikibase:quantityAmount ?lengthValue }
              OPTIONAL { { ?item rdfs:label ?name FILTER(LANG(?name) IN ("en", "fr", "mul")) }
                UNION { ?item wdt:P2561|wdt:P1448 ?name } }
            }
            GROUP BY ?mmsi ?item
            """.trimIndent()

        /** Names are kept as they are: the vessel's own name is left out by the import, which knows it. */
        fun parse(response: SparqlResponse): Map<String, WikidataData> =
            response.results.bindings
                .mapNotNull { row ->
                    val mmsi = row["mmsi"]?.value ?: return@mapNotNull null
                    val id = row["item"]?.value?.let { ITEM_ID.find(it)?.value } ?: return@mapNotNull null

                    mmsi to
                        WikidataData(
                            id = id,
                            imo = row["imo"]?.value?.takeIf { IMO.matches(it) },
                            shipTypeEn = row["typeEn"]?.value?.take(100),
                            shipTypeFr = row["typeFr"]?.value?.take(100),
                            yearBuilt =
                                (row["yearBuilt"] ?: row["yearInService"])
                                    ?.value
                                    ?.toIntOrNull()
                                    ?.takeIf { it in 1800..2100 },
                            lengthMeters =
                                row["length"]
                                    ?.value
                                    ?.toDoubleOrNull()
                                    ?.takeIf { it > 0 && it < 10_000 },
                            otherNames =
                                row["names"]
                                    ?.value
                                    ?.split("|")
                                    ?.map { it.trim() }
                                    ?.filter { it.isNotEmpty() }
                                    ?.distinctBy { it.lowercase() }
                                    .orEmpty(),
                        )
                }
                // An MMSI on two items (e.g. a ship and its namesake): neither is trusted
                .groupBy({ it.first }, { it.second })
                .filterValues { it.size == 1 }
                .mapValues { it.value.single() }
    }
}
