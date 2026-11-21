package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.HomePortCorrection
import org.openvesselphotos.domain.entities.ReviewStatus

interface HomePortCorrectionRepository {
    fun findById(id: Int): HomePortCorrection?

    fun findByStatus(status: ReviewStatus): List<HomePortCorrection>

    fun findByCfrAndStatus(
        cfr: String,
        status: ReviewStatus,
    ): List<HomePortCorrection>

    fun save(correction: HomePortCorrection): HomePortCorrection
}
