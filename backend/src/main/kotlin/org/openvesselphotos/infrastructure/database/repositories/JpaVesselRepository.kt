package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.Vessel
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.repositories.VesselRepository
import org.openvesselphotos.infrastructure.database.entities.VesselEntity
import org.openvesselphotos.infrastructure.database.repositories.interfaces.DBVesselRepository
import org.springframework.stereotype.Repository

@Repository
class JpaVesselRepository(
    private val dbVesselRepository: DBVesselRepository,
) : VesselRepository {
    override fun findById(id: Int): Vessel? = dbVesselRepository.findById(id).orElse(null)?.toVessel()

    override fun findByKey(key: VesselKey): Vessel? =
        when (key.type) {
            KeyType.CFR -> dbVesselRepository.findByCfr(key.value)
            KeyType.MMSI -> dbVesselRepository.findByMmsi(key.value)
        }?.toVessel()

    override fun findByIds(ids: Collection<Int>): List<Vessel> =
        dbVesselRepository.findAllById(ids).map { it.toVessel() }

    override fun search(
        query: String,
        limit: Int,
    ): List<Vessel> {
        val escapedQuery = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val keyPrefix = VesselKey.normalize(escapedQuery)

        return dbVesselRepository.search(escapedQuery, keyPrefix, limit).map { it.toVessel() }
    }

    override fun findRecentlyPhotographed(limit: Int): List<Vessel> =
        dbVesselRepository.findRecentlyPhotographed(limit).map { it.toVessel() }

    override fun findAllWithApprovedPhotos(): List<Vessel> =
        dbVesselRepository.findAllWithApprovedPhotos().map {
            it.toVessel()
        }

    override fun save(vessel: Vessel): Vessel {
        val saved = dbVesselRepository.saveAndFlush(VesselEntity.fromVessel(vessel))

        // Reload to get database defaults (created_at)
        return findById(requireNotNull(saved.id)) ?: saved.toVessel()
    }
}
