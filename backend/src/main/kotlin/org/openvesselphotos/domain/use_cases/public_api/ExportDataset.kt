package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.repositories.VesselRepository

@UseCase
class ExportDataset(
    private val vesselRepository: VesselRepository,
    private val vesselPhotosLoader: VesselPhotosLoader,
) {
    fun execute(): List<VesselWithPhotos> = vesselPhotosLoader.load(vesselRepository.findAllWithApprovedPhotos())
}
