package org.openvesselphotos.domain.use_cases.public_api

import org.openvesselphotos.config.UseCase
import org.openvesselphotos.domain.entities.VesselFilter
import org.openvesselphotos.domain.entities.VesselFilterOptions
import org.openvesselphotos.domain.entities.VesselWithPhotos
import org.openvesselphotos.domain.repositories.VesselRepository
import java.time.LocalDate

/**
 * Vessels with approved photos by name, page by page: the sitemap and the vessel directory (pages without photo are
 * not indexed, but a few described by the EU fleet register: see [GetVesselsWithoutPhotosToIndex]).
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

    /**
     * For each page from 1, the last day it changed. A vessel published in a page moves the following vessels down,
     * to the next pages: a page changes with any photo published in it or before it.
     */
    fun lastModifiedByPage(pageSize: Int): List<LocalDate> {
        val days = vesselRepository.findLastPhotoDayByPage(pageSize)

        return (1..(days.keys.maxOrNull() ?: 0))
            .runningFold(null as LocalDate?) { last, page -> listOfNotNull(last, days[page]).maxOrNull() }
            .drop(1)
            .filterNotNull()
    }

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
