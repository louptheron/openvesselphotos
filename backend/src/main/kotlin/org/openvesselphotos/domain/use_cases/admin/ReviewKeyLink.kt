package org.openvesselphotos.domain.use_cases.admin

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.KeyLinkRequest
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.KeyLinkRequestRepository
import org.openvesselphotos.domain.repositories.VesselRepository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@UseCase
class ReviewKeyLink(
    private val vesselRepository: VesselRepository,
    private val keyLinkRequestRepository: KeyLinkRequestRepository,
) {
    @Transactional
    fun execute(
        requestId: Int,
        approve: Boolean,
    ): KeyLinkRequest {
        val request =
            keyLinkRequestRepository.findById(requestId)
                ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No key link request $requestId.")
        if (request.status != ReviewStatus.PENDING) {
            throw BackendUsageException(
                BackendUsageErrorCode.CONFLICT,
                "Key link request $requestId was already reviewed.",
            )
        }

        if (approve) {
            val vessel =
                vesselRepository.findById(request.vesselId)
                    ?: throw BackendUsageException(BackendUsageErrorCode.NOT_FOUND, "No vessel ${request.vesselId}.")
            if (vessel.keyValue(request.key.type) != null) {
                throw BackendUsageException(
                    BackendUsageErrorCode.CONFLICT,
                    "This vessel already has a ${request.key.type.name}.",
                )
            }
            if (vesselRepository.findByKey(request.key) != null) {
                throw BackendUsageException(
                    BackendUsageErrorCode.CONFLICT,
                    "${request.key.type.name} ${request.key.value} already belongs to another vessel: reject this request.",
                )
            }
            vesselRepository.save(vessel.withKey(request.key))
        }

        return keyLinkRequestRepository.save(
            request.copy(
                status = if (approve) ReviewStatus.APPROVED else ReviewStatus.REJECTED,
                reviewedAt = ZonedDateTime.now(),
            ),
        )
    }
}
