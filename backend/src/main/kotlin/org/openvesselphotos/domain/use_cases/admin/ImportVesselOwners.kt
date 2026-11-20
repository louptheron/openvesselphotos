package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.DataSource
import org.openvesselphotos.domain.entities.DataUpdate
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.entities.VesselOwner
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.DataUpdateRepository
import org.openvesselphotos.domain.repositories.PortRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** A vessel's owner read from an import file, before validation. */
data class VesselOwnerImportRow(
    val line: Int,
    val cfr: String?,
    val owner: String?,
    val group: String?,
    val sourceUrl: String?,
)

data class VesselOwnerImportResult(
    val read: Int,
    val imported: Int,
    val skipped: Int,
    val errors: List<String>,
)

/**
 * Replaces the owners of the vessels (armements), shown on the ports site. Each needs a public source: the link is
 * shown with the owner.
 */
@UseCase
class ImportVesselOwners(
    private val portRepository: PortRepository,
    private val dataUpdateRepository: DataUpdateRepository,
) {
    @Transactional
    fun execute(rows: List<VesselOwnerImportRow>): VesselOwnerImportResult {
        val errors = mutableListOf<String>()
        val owners = linkedMapOf<String, VesselOwner>()
        rows.forEach { row ->
            try {
                val cfr = VesselKey.of(KeyType.CFR, row.cfr.orEmpty()).value
                val owner = row.owner.clean(100)
                val sourceUrl = row.sourceUrl?.trim()?.takeIf { it.startsWith("https://") && it.length <= 500 }
                when {
                    owner == null -> errors += "Line ${row.line}: no owner."
                    sourceUrl == null -> errors += "Line ${row.line}: no https:// source."
                    else -> owners[cfr] = VesselOwner(cfr, owner, row.group.clean(100), sourceUrl)
                }
            } catch (e: BackendUsageException) {
                errors += "Line ${row.line}: ${e.message}"
            }
        }
        portRepository.replaceOwners(owners.values.toList())
        if (owners.isNotEmpty()) {
            dataUpdateRepository.add(
                DataUpdate(DataSource.VESSEL_OWNERS, LocalDate.now(), "Armateurs de ${owners.size} navires"),
            )
        }

        return VesselOwnerImportResult(
            read = rows.size,
            imported = owners.size,
            skipped = errors.size,
            errors = errors.take(ImportVessels.MAX_ERRORS),
        )
    }
}
