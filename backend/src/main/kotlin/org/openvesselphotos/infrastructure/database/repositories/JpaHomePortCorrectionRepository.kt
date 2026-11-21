package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.HomePortCorrection
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.domain.repositories.HomePortCorrectionRepository
import org.openvesselphotos.infrastructure.database.entities.HomePortCorrectionEntity
import org.openvesselphotos.infrastructure.database.repositories.interfaces.DBHomePortCorrectionRepository
import org.springframework.stereotype.Repository

@Repository
class JpaHomePortCorrectionRepository(
    private val dbHomePortCorrectionRepository: DBHomePortCorrectionRepository,
) : HomePortCorrectionRepository {
    override fun findById(id: Int): HomePortCorrection? =
        dbHomePortCorrectionRepository.findById(id).orElse(null)?.toHomePortCorrection()

    override fun findByStatus(status: ReviewStatus): List<HomePortCorrection> =
        dbHomePortCorrectionRepository.findByStatusOrderByCreatedAt(status).map { it.toHomePortCorrection() }

    override fun findByCfrAndStatus(
        cfr: String,
        status: ReviewStatus,
    ): List<HomePortCorrection> =
        dbHomePortCorrectionRepository.findByCfrAndStatus(cfr, status).map { it.toHomePortCorrection() }

    override fun save(correction: HomePortCorrection): HomePortCorrection =
        dbHomePortCorrectionRepository
            .saveAndFlush(HomePortCorrectionEntity.fromHomePortCorrection(correction))
            .toHomePortCorrection()
}
