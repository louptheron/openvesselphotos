package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Licence
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.images.ImageDownloader
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZonedDateTime

/** A photo read from an import file, before validation. */
data class PhotoImportRow(
    val line: Int,
    val cfr: String?,
    val mmsi: String?,
    val vesselName: String?,
    val credit: String?,
    val licence: String?,
    val takenOn: String?,
    val sourceUrl: String?,
    val imageUrl: String?,
)

data class PhotoImportStatus(
    val isRunning: Boolean,
    val total: Int,
    val done: Int,
    val imported: Int,
    /** Already imported by a previous run, e.g. one interrupted by a restart */
    val alreadyImported: Int,
    val failed: Int,
    /** Lines rejected before the import started */
    val invalid: Int,
    /** The first errors, for the admin to fix the file */
    val errors: List<String>,
    val startedAt: ZonedDateTime?,
    val finishedAt: ZonedDateTime?,
)

/**
 * Imports openly licensed photos (e.g. from Wikimedia Commons or Flickr) and publishes them without review. The
 * images are downloaded one at a time, in the background: the admin follows the progress with [status]. Photos are
 * recognized by their source page, so that an interrupted import can be started again with the same file.
 */
@UseCase
class ImportPhotos(
    private val imageDownloader: ImageDownloader,
    private val photoRepository: PhotoRepository,
    private val publishImportedPhoto: PublishImportedPhoto,
) {
    private val logger = LoggerFactory.getLogger(ImportPhotos::class.java)

    @Volatile
    private var current = PhotoImportStatus(false, 0, 0, 0, 0, 0, 0, emptyList(), null, null)

    @Volatile
    private var isCancelled = false

    fun status(): PhotoImportStatus = current

    @Synchronized
    fun start(rows: List<PhotoImportRow>): PhotoImportStatus {
        if (current.isRunning) {
            throw BackendUsageException(BackendUsageErrorCode.CONFLICT, "An import is already running.")
        }
        val errors = mutableListOf<String>()
        val photos =
            rows.mapNotNull { row ->
                try {
                    validate(row)
                } catch (e: BackendUsageException) {
                    errors += "Line ${row.line}: ${e.message}"
                    null
                }
            }
        isCancelled = false
        current =
            PhotoImportStatus(
                isRunning = photos.isNotEmpty(),
                total = photos.size,
                done = 0,
                imported = 0,
                alreadyImported = 0,
                failed = 0,
                invalid = errors.size,
                errors = errors.take(MAX_ERRORS),
                startedAt = ZonedDateTime.now(),
                finishedAt = if (photos.isEmpty()) ZonedDateTime.now() else null,
            )
        if (photos.isNotEmpty()) {
            Thread.ofVirtual().name("photo-import").start { run(photos) }
        }

        return current
    }

    fun cancel() {
        isCancelled = true
    }

    private fun run(photos: List<PhotoImport>) {
        logger.info("Importing ${photos.size} photos.")
        photos.forEach { photo ->
            if (isCancelled) {
                return@forEach
            }
            var status = current
            try {
                if (photoRepository.existsBySourceUrl(photo.sourceUrl)) {
                    status = status.copy(alreadyImported = status.alreadyImported + 1)
                } else {
                    publishImportedPhoto.execute(photo, imageDownloader.download(photo.imageUrl))
                    status = status.copy(imported = status.imported + 1)
                    Thread.sleep(DELAY_BETWEEN_DOWNLOADS_MS)
                }
            } catch (e: Exception) {
                logger.warn("Could not import line ${photo.line} (${photo.imageUrl}): ${e.message}")
                status =
                    status.copy(
                        failed = status.failed + 1,
                        errors = (status.errors + "Line ${photo.line}: ${e.message}").take(MAX_ERRORS),
                    )
            }
            current = status.copy(done = status.done + 1)
        }
        current = current.copy(isRunning = false, finishedAt = ZonedDateTime.now())
        logger.info("Photo import finished: $current")
    }

    private fun validate(row: PhotoImportRow): PhotoImport {
        val cfr = row.cfr?.takeIf { it.isNotBlank() }?.let { VesselKey.of(KeyType.CFR, it) }
        val mmsi = row.mmsi?.takeIf { it.isNotBlank() }?.let { VesselKey.of(KeyType.MMSI, it) }
        val keys = listOfNotNull(cfr, mmsi).ifEmpty { throw invalid("no CFR nor MMSI.") }
        val credit = row.credit.clean() ?: throw invalid("no author.")
        if (credit.length > MAX_CREDIT_LENGTH) {
            throw invalid("the author is longer than $MAX_CREDIT_LENGTH characters.")
        }
        val licence = Licence.fromSpdx(row.licence.clean() ?: throw invalid("no licence."))
        val sourceUrl = row.sourceUrl.clean()?.takeIf { it.isHttpUrl() } ?: throw invalid("no source page URL.")
        if (sourceUrl.length > MAX_URL_LENGTH) {
            throw invalid("the source page URL is too long.")
        }
        val imageUrl = row.imageUrl.clean()?.takeIf { it.isHttpUrl() } ?: throw invalid("no image URL.")
        // Commons dates can be "2012-08-25 14:03:11" or free text: only a leading ISO date is kept
        val takenOn =
            row.takenOn
                ?.let { Regex("^\\d{4}-\\d{2}-\\d{2}").find(it.trim())?.value }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.takeIf { !it.isAfter(LocalDate.now()) }

        return PhotoImport(
            line = row.line,
            keys = keys,
            vesselName = row.vesselName.clean()?.take(100),
            credit = credit,
            licence = licence,
            takenOn = takenOn,
            sourceUrl = sourceUrl,
            imageUrl = imageUrl,
        )
    }

    private fun String?.clean(): String? = this?.trim()?.replace(Regex("\\s+"), " ")?.ifEmpty { null }

    private fun String.isHttpUrl() = startsWith("https://") || startsWith("http://")

    private fun invalid(message: String) = BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, message)

    companion object {
        const val MAX_ERRORS = 50
        const val MAX_CREDIT_LENGTH = 100
        const val MAX_URL_LENGTH = 500

        /** Keeps the import polite with the hosts it downloads from */
        const val DELAY_BETWEEN_DOWNLOADS_MS = 200L
    }
}
