package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.VesselRepository
import org.springframework.transaction.annotation.Transactional

/** A photo note read from a file, before validation. */
data class PhotoNoteRow(
    val line: Int,
    val cfr: String?,
    val mmsi: String?,
    /** The photo's public number `n` */
    val number: String?,
    /** Empty to remove the note */
    val note: String?,
)

data class PhotoNotesResult(
    val read: Int,
    val updated: Int,
    val unchanged: Int,
    /** The first errors, for the admin to fix the file */
    val errors: List<String>,
)

/** Sets the notes shown with published photos, e.g. "Photographed under its former name LE CRI DES FLOTS". */
@UseCase
class SetPhotoNotes(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
) {
    @Transactional
    fun execute(rows: List<PhotoNoteRow>): PhotoNotesResult {
        val errors = mutableListOf<String>()
        var updated = 0
        var unchanged = 0
        rows.forEach { row ->
            try {
                val key =
                    row.cfr?.takeIf { it.isNotBlank() }?.let { VesselKey.of(KeyType.CFR, it) }
                        ?: row.mmsi?.takeIf { it.isNotBlank() }?.let { VesselKey.of(KeyType.MMSI, it) }
                if (key == null) {
                    errors += "Line ${row.line}: no CFR nor MMSI."
                    return@forEach
                }
                val number = row.number?.trim()?.toIntOrNull()
                if (number == null) {
                    errors += "Line ${row.line}: no photo number."
                    return@forEach
                }
                val note =
                    row.note
                        ?.trim()
                        ?.replace(Regex("\\s+"), " ")
                        ?.ifEmpty { null }
                if (note != null && note.length > MAX_NOTE_LENGTH) {
                    errors += "Line ${row.line}: the note is longer than $MAX_NOTE_LENGTH characters."
                    return@forEach
                }
                val photo =
                    vesselRepository
                        .findByKey(key)
                        ?.id
                        ?.let { photoRepository.findApprovedByVesselIdAndNumber(it, number) }
                if (photo == null) {
                    errors += "Line ${row.line}: no photo $number for ${key.type.name} ${key.value}."
                    return@forEach
                }
                if (photo.note == note) {
                    unchanged++
                } else {
                    photoRepository.save(photo.copy(note = note))
                    updated++
                }
            } catch (e: BackendUsageException) {
                errors += "Line ${row.line}: ${e.message}"
            }
        }

        return PhotoNotesResult(
            read = rows.size,
            updated = updated,
            unchanged = unchanged,
            errors = errors.take(MAX_ERRORS),
        )
    }

    companion object {
        const val MAX_NOTE_LENGTH = 200
        const val MAX_ERRORS = 50
    }
}
