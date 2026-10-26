package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.WikidataData
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.VesselRepository
import org.openvesselphotos.domain.wikidata.WikidataClient
import org.slf4j.LoggerFactory
import java.text.Normalizer
import java.time.LocalDate
import java.time.ZonedDateTime

data class WikidataImportStatus(
    val isRunning: Boolean,
    /** Vessels with photos and an MMSI */
    val total: Int,
    val done: Int,
    /** Vessels found on Wikidata */
    val found: Int,
    val failed: Int,
    val errors: List<String>,
    val startedAt: ZonedDateTime?,
    val finishedAt: ZonedDateTime?,
)

/**
 * Completes the vessels with photos with what Wikidata knows of them (IMO number, type, year, length, other names),
 * found by their MMSI: most vessels outside the EU fleet register have nothing else. Runs in the background, by
 * batches; the admin follows the progress with [status]. Vessels Wikidata does not know keep their data.
 */
@UseCase
class ImportWikidata(
    private val vesselRepository: VesselRepository,
    private val wikidataClient: WikidataClient,
) {
    private val logger = LoggerFactory.getLogger(ImportWikidata::class.java)

    @Volatile
    private var current = WikidataImportStatus(false, 0, 0, 0, 0, emptyList(), null, null)

    fun status(): WikidataImportStatus = current

    @Synchronized
    fun start(): WikidataImportStatus {
        if (current.isRunning) {
            throw BackendUsageException(BackendUsageErrorCode.CONFLICT, "A Wikidata import is already running.")
        }
        val vessels = vesselRepository.findAllWithApprovedPhotos().filter { it.mmsi != null }
        current =
            WikidataImportStatus(
                isRunning = vessels.isNotEmpty(),
                total = vessels.size,
                done = 0,
                found = 0,
                failed = 0,
                errors = emptyList(),
                startedAt = ZonedDateTime.now(),
                finishedAt = if (vessels.isEmpty()) ZonedDateTime.now() else null,
            )
        if (vessels.isNotEmpty()) {
            Thread.ofVirtual().name("wikidata-import").start { run(vessels) }
        }

        return current
    }

    private fun run(vessels: List<Vessel>) {
        logger.info("Looking for ${vessels.size} vessels on Wikidata.")
        vessels.chunked(BATCH_SIZE).forEach { batch ->
            var status = current
            try {
                val found = wikidataClient.findByMmsi(batch.mapNotNull { it.mmsi })
                val updates =
                    batch.mapNotNull { vessel ->
                        found[vessel.mmsi]?.let { requireNotNull(vessel.id) to withoutOwnName(it, vessel) }
                    }
                vesselRepository.updateWikidata(updates.toMap())
                status = status.copy(found = status.found + updates.size)
                Thread.sleep(DELAY_BETWEEN_QUERIES_MS)
            } catch (e: Exception) {
                logger.warn("Could not query Wikidata: ${e.message}")
                status =
                    status.copy(
                        failed = status.failed + batch.size,
                        errors = (status.errors + "${e.message}").take(ImportPhotos.MAX_ERRORS),
                    )
            }
            current = status.copy(done = status.done + batch.size)
        }
        current = current.copy(isRunning = false, finishedAt = ZonedDateTime.now())
        logger.info("Wikidata import finished: $current")
    }

    /**
     * Leaves out the names that are the vessel's own, or close to it, e.g. "GU-74 Katie C" for "Katie C": only the
     * names under which people may also look for it remain, e.g. a former name.
     */
    private fun withoutOwnName(
        data: WikidataData,
        vessel: Vessel,
    ): WikidataData {
        val ownName = vessel.name?.let { normalize(it) }
        var length = 0
        val otherNames =
            data.otherNames
                .filter { name ->
                    val other = normalize(name)
                    other.isNotEmpty() &&
                        (ownName.isNullOrEmpty() || (!other.contains(ownName) && !ownName.contains(other)))
                }.distinctBy { normalize(it) }
                .map { it.take(MAX_NAME_LENGTH) }
                .takeWhile {
                    length += it.length + 1
                    length <= MAX_NAMES_LENGTH
                }.take(MAX_NAMES)

        return data.copy(otherNames = otherNames, importedOn = LocalDate.now())
    }

    private fun normalize(name: String): String =
        Normalizer
            .normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    companion object {
        /** MMSIs per query: well below the query service's limits */
        const val BATCH_SIZE = 200

        /** Keeps the import polite with the Wikidata Query Service */
        const val DELAY_BETWEEN_QUERIES_MS = 1_000L

        private const val MAX_NAMES = 5
        private const val MAX_NAME_LENGTH = 100

        /** Below the size of the other_names column */
        private const val MAX_NAMES_LENGTH = 500
    }
}
