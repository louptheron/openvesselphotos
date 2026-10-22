package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.repositories.VesselRepository

/**
 * Vessels with approved photos by name, page by page: the sitemap and the vessel directory (pages without photo are
 * not indexed).
 */
@UseCase
class GetVesselsPage(
    private val vesselRepository: VesselRepository,
    private val vesselPhotosLoader: VesselPhotosLoader,
) {
    fun count(): Int = vesselRepository.countWithApprovedPhotos()

    fun pageCount(pageSize: Int): Int = (count() + pageSize - 1) / pageSize

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

    companion object {
        /** Vessels per page of the directory (/vessels) */
        const val DIRECTORY_PAGE_SIZE = 100
    }
}
