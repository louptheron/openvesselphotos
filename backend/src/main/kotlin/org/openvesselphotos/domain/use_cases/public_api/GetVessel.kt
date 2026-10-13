package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.HistoryEvent
import org.openvesselphotos.domain.entities.KeyType
import org.openvesselphotos.domain.entities.VesselDetails
import org.openvesselphotos.domain.entities.VesselKey
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.repositories.KeyLinkRequestRepository
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.VesselRepository

@UseCase
class GetVessel(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
    private val keyLinkRequestRepository: KeyLinkRequestRepository,
) {
    fun execute(key: VesselKey): VesselDetails {
        val vessel =
            vesselRepository.findByKey(key)
                ?: throw BackendUsageException(
                    BackendUsageErrorCode.NOT_FOUND,
                    "No vessel with ${key.type.name} ${key.value}.",
                )
        val vesselId = requireNotNull(vessel.id)
        val photos = photoRepository.findApprovedByVesselIds(listOf(vesselId))

        val photoEvents =
            photos.mapNotNull { photo ->
                photo.reviewedAt?.let {
                    HistoryEvent(
                        it,
                        "Photo ${photo.number} published (${photo.licence.label})",
                        photo.credit,
                    )
                }
            }
        val linkEvents =
            keyLinkRequestRepository.findApprovedByVesselId(vesselId).mapNotNull { request ->
                val otherType = if (request.key.type == KeyType.CFR) KeyType.MMSI else KeyType.CFR
                val otherKey = vessel.keyValue(otherType)?.let { " to ${otherType.name} $it" } ?: ""
                request.reviewedAt?.let {
                    HistoryEvent(
                        it,
                        "${request.key.type.name} ${request.key.value} linked$otherKey",
                        request.contributor,
                    )
                }
            }
        val creationEvent = vessel.createdAt?.let { HistoryEvent(it, "Vessel added", null) }

        return VesselDetails(
            vessel = vessel,
            photos = photos,
            history = (photoEvents + linkEvents + listOfNotNull(creationEvent)).sortedByDescending { it.date },
        )
    }
}
