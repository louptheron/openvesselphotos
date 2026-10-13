package org.openvesselphotos.domain.repositories

import org.openvesselphotos.domain.entities.KeyLinkRequest
import org.openvesselphotos.domain.entities.ReviewStatus

interface KeyLinkRequestRepository {
    fun findById(id: Int): KeyLinkRequest?

    fun findByStatus(status: ReviewStatus): List<KeyLinkRequest>

    fun findApprovedByVesselId(vesselId: Int): List<KeyLinkRequest>

    fun save(request: KeyLinkRequest): KeyLinkRequest
}
