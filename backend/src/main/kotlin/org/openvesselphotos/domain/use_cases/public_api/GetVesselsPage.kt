package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.VesselFilter
import org.openvesselphotos.domain.entities.VesselFilterOptions
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
    fun count(filter: VesselFilter = VesselFilter()): Int = vesselRepository.countWithApprovedPhotos(filter)

    fun pageCount(
        pageSize: Int,
        filter: VesselFilter = VesselFilter(),
    ): Int = (count(filter) + pageSize - 1) / pageSize

    /** [page] starts at 1. */
    fun execute(
        page: Int,
        pageSize: Int,
        filter: VesselFilter = VesselFilter(),
    ): List<VesselWithPhotos> =
        vesselPhotosLoader.load(
            vesselRepository.findPageWithApprovedPhotos(
                (page - 1) * pageSize,
                pageSize,
                filter,
            ),
        )

    /** The values the directory can be filtered by */
    fun filterOptions(): VesselFilterOptions = vesselRepository.findFilterOptions()

    companion object {
        /** Vessels per page of the directory (/vessels) */
        const val DIRECTORY_PAGE_SIZE = 100
    }
}

/** A directory filter from query parameters: blank values are ignored, the flag is upper-cased. */
fun vesselFilter(
    flag: String?,
    port: String?,
    vesselType: String?,
    mainGear: String?,
) = VesselFilter(
    flag = flag?.trim()?.uppercase()?.ifEmpty { null },
    port = port?.trim()?.ifEmpty { null },
    vesselType = vesselType?.trim()?.ifEmpty { null },
    mainGear = mainGear?.trim()?.ifEmpty { null },
)
