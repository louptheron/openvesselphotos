package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.repositories.VesselRepository

/** All vessels, with or without photos, page by page (for the sitemap). */
@UseCase
class GetVesselsPage(
    private val vesselRepository: VesselRepository,
    private val vesselPhotosLoader: VesselPhotosLoader,
) {
    fun count(): Int = vesselRepository.count()

    /** [page] starts at 1. */
    fun execute(
        page: Int,
        pageSize: Int,
    ): List<VesselWithPhotos> = vesselPhotosLoader.load(vesselRepository.findPage((page - 1) * pageSize, pageSize))
}
