package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.repositories.VesselRepository

/** Vessels with approved photos, page by page (for the sitemap: pages without photo are not indexed). */
@UseCase
class GetVesselsPage(
    private val vesselRepository: VesselRepository,
    private val vesselPhotosLoader: VesselPhotosLoader,
) {
    fun count(): Int = vesselRepository.countWithApprovedPhotos()

    /** [page] starts at 1. */
    fun execute(
        page: Int,
        pageSize: Int,
    ): List<VesselWithPhotos> =
        vesselPhotosLoader.load(
            vesselRepository.findPageWithApprovedPhotos(
                (page - 1) * pageSize,
                pageSize,
            ),
        )
}
