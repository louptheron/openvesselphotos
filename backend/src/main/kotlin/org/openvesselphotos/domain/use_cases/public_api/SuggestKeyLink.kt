package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.KeyLinkRequest
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.KeyLinkRequestRepository
import org.openvesselphotos.domain.repositories.VesselRepository

@UseCase
class SuggestKeyLink(
    private val vesselRepository: VesselRepository,
    private val keyLinkRequestRepository: KeyLinkRequestRepository,
) {
    fun execute(
        vesselKey: VesselKey,
        missingKey: VesselKey,
        contributor: String?,
    ): KeyLinkRequest {
        val vessel =
            vesselRepository.findByKey(vesselKey)
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.NOT_FOUND,
                    "No vessel with ${vesselKey.type.name} ${vesselKey.value}.",
                )
        if (vessel.keyValue(missingKey.type) != null) {
            throw BackendUsageException(
                BackendUsageErrorCode.CONFLICT,
                "This vessel already has a ${missingKey.type.name}.",
            )
        }
        if (vesselRepository.findByKey(missingKey) != null) {
            throw BackendUsageException(
                BackendUsageErrorCode.CONFLICT,
                "${missingKey.type.name} ${missingKey.value} belongs to another vessel.",
            )
        }

        return keyLinkRequestRepository.save(
            KeyLinkRequest(
                vesselId = requireNotNull(vessel.id),
                key = missingKey,
                contributor = contributor?.trim()?.take(100)?.ifEmpty { null },
                status = ReviewStatus.PENDING,
            ),
        )
    }
}
