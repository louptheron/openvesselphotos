package org.openvesselphotos.infrastructure.database.repositories

import org.openvesselphotos.domain.entities.KeyLinkRequest
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.domain.repositories.KeyLinkRequestRepository
import org.openvesselphotos.infrastructure.database.entities.KeyLinkRequestEntity
import org.openvesselphotos.infrastructure.database.repositories.interfaces.DBKeyLinkRequestRepository
import org.springframework.stereotype.Repository

@Repository
class JpaKeyLinkRequestRepository(
    private val dbKeyLinkRequestRepository: DBKeyLinkRequestRepository,
) : KeyLinkRequestRepository {
    override fun findById(id: Int): KeyLinkRequest? =
        dbKeyLinkRequestRepository.findById(id).orElse(null)?.toKeyLinkRequest()

    override fun findByStatus(status: ReviewStatus): List<KeyLinkRequest> =
        dbKeyLinkRequestRepository.findByStatusOrderByCreatedAt(status).map { it.toKeyLinkRequest() }

    override fun findApprovedByVesselId(vesselId: Int): List<KeyLinkRequest> =
        dbKeyLinkRequestRepository
            .findByVesselIdAndStatus(
                vesselId,
                ReviewStatus.APPROVED,
            ).map { it.toKeyLinkRequest() }

    override fun save(request: KeyLinkRequest): KeyLinkRequest =
        dbKeyLinkRequestRepository.saveAndFlush(KeyLinkRequestEntity.fromKeyLinkRequest(request)).toKeyLinkRequest()
}
