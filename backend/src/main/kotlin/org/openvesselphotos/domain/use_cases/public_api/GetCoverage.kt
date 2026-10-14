package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.Coverage
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.repositories.PhotoRepository
import org.openvesselphotos.domain.repositories.VesselRepository

@UseCase
class GetCoverage(
    private val vesselRepository: VesselRepository,
    private val photoRepository: PhotoRepository,
) {
    fun execute(): Coverage {
        val counts = vesselRepository.countCoverage()

        return Coverage(
            registerVessels = counts.registerVessels,
            registerPhotographed = counts.registerPhotographed,
            photographedVessels = counts.photographedVessels,
            photos = photoRepository.countByStatus(PhotoStatus.APPROVED),
        )
    }
}
