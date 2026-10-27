package org.openvesselphotos.infrastructure.database.repositories.interfaces

import org.openvesselphotos.domain.entities.ApiKeyStatus
import org.openvesselphotos.domain.entities.PhotoStatus
import org.openvesselphotos.domain.entities.ReportStatus
import org.openvesselphotos.domain.entities.ReviewStatus
import org.openvesselphotos.infrastructure.database.entities.ApiKeyEntity
import org.openvesselphotos.infrastructure.database.entities.KeyLinkRequestEntity
import org.openvesselphotos.infrastructure.database.entities.PhotoEntity
import org.openvesselphotos.infrastructure.database.entities.PhotoFileEntity
import org.openvesselphotos.infrastructure.database.entities.PhotoFileId
import org.openvesselphotos.infrastructure.database.entities.ReportEntity
import org.openvesselphotos.infrastructure.database.entities.VesselEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

/**
 * Filters of the vessel directory, null for any value. The flag comes from the CFR, else from the MMSI's MID: [mids]
 * lists the flag's MIDs, comma-separated.
 */
private const val DIRECTORY_FILTER = """
    AND (CAST(:flag AS TEXT) IS NULL
        OR (v.cfr IS NOT NULL AND left(v.cfr, 3) = CAST(:flag AS TEXT))
        OR (v.cfr IS NULL AND left(v.mmsi, 3) = ANY(string_to_array(CAST(:mids AS TEXT), ','))))
    AND (CAST(:port AS TEXT) IS NULL OR v.port = CAST(:port AS TEXT))
    AND (CAST(:vesselType AS TEXT) IS NULL OR v.vessel_type = CAST(:vesselType AS TEXT))
    AND (CAST(:mainGear AS TEXT) IS NULL OR v.main_gear = CAST(:mainGear AS TEXT))
"""

private const val WITH_APPROVED_PHOTOS =
    "FROM vessels v WHERE EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED')"

/** Same rule as Vessel.isDescribed */
private const val DESCRIBED_WITHOUT_APPROVED_PHOTOS =
    "FROM vessels v WHERE v.name IS NOT NULL AND v.vessel_type IS NOT NULL AND v.vessel_type <> 'FX' " +
        "AND v.year_built IS NOT NULL AND v.length_m IS NOT NULL " +
        "AND NOT EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED')"

private const val COUNT_DIRECTORY = "SELECT count(*) $WITH_APPROVED_PHOTOS $DIRECTORY_FILTER"

private const val FIND_DIRECTORY_PAGE =
    "SELECT * $WITH_APPROVED_PHOTOS $DIRECTORY_FILTER ORDER BY v.name NULLS LAST, v.id OFFSET :offset LIMIT :limit"

interface FilterOptionCount {
    val kind: String
    val value: String
    val count: Long
}

interface DBVesselRepository : JpaRepository<VesselEntity, Int> {
    fun findByCfr(cfr: String): VesselEntity?

    fun findByMmsi(mmsi: String): VesselEntity?

    @Query(
        value = """
            SELECT * FROM vessels
            WHERE name ILIKE '%' || :query || '%' OR cfr LIKE :keyPrefix || '%' OR mmsi LIKE :keyPrefix || '%'
               OR replace(registration, ' ', '') ILIKE :keyPrefix || '%'
               OR port ILIKE :query || '%'
            ORDER BY name NULLS LAST
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun search(
        query: String,
        keyPrefix: String,
        limit: Int,
    ): List<VesselEntity>

    @Query(
        value = """
            SELECT v.* FROM vessels v
            JOIN photos p ON p.vessel_id = v.id AND p.status = 'APPROVED'
            GROUP BY v.id
            ORDER BY max(p.reviewed_at) DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findRecentlyPhotographed(limit: Int): List<VesselEntity>

    @Query(
        value = """
            SELECT * FROM vessels v
            WHERE EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED')
            ORDER BY v.id
        """,
        nativeQuery = true,
    )
    fun findAllWithApprovedPhotos(): List<VesselEntity>

    @Query("SELECT * $DESCRIBED_WITHOUT_APPROVED_PHOTOS ORDER BY v.id LIMIT :limit", nativeQuery = true)
    fun findDescribedWithoutApprovedPhotos(limit: Int): List<VesselEntity>

    @Query("SELECT count(*) $DESCRIBED_WITHOUT_APPROVED_PHOTOS AND v.id < :id", nativeQuery = true)
    fun countDescribedWithoutApprovedPhotosBefore(id: Int): Long

    @Query(value = COUNT_DIRECTORY, nativeQuery = true)
    fun countWithApprovedPhotos(
        flag: String?,
        mids: String?,
        port: String?,
        vesselType: String?,
        mainGear: String?,
    ): Long

    @Query(value = FIND_DIRECTORY_PAGE, nativeQuery = true)
    fun findPageWithApprovedPhotos(
        offset: Int,
        limit: Int,
        flag: String?,
        mids: String?,
        port: String?,
        vesselType: String?,
        mainGear: String?,
    ): List<VesselEntity>

    /** Per filter ("flag", "port", "vesselType", "mainGear"), the number of vessels with photos by value */
    @Query(
        value = """
            WITH v AS (
                SELECT * FROM vessels v
                WHERE EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED')
            )
            SELECT 'flag' AS kind, CASE WHEN cfr IS NOT NULL THEN left(cfr, 3) ELSE 'MID' || left(mmsi, 3) END AS value,
                count(*) AS count
            FROM v GROUP BY 2
            UNION ALL
            SELECT 'port', port, count(*) FROM v WHERE port IS NOT NULL GROUP BY 2
            UNION ALL
            SELECT 'vesselType', vessel_type, count(*) FROM v WHERE vessel_type IS NOT NULL GROUP BY 2
            UNION ALL
            SELECT 'mainGear', main_gear, count(*) FROM v WHERE main_gear IS NOT NULL GROUP BY 2
        """,
        nativeQuery = true,
    )
    fun countFilterOptions(): List<FilterOptionCount>

    /** See [org.openvesselphotos.domain.repositories.VesselRepository.findRelatedWithApprovedPhotos] */
    @Query(
        value = """
            SELECT * FROM vessels v
            WHERE v.id <> :id
              AND EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED')
              AND (v.port = CAST(:port AS TEXT) OR left(v.cfr, 3) = CAST(:flag AS TEXT)
                OR left(v.mmsi, 3) = CAST(:mid AS TEXT))
            ORDER BY v.port = CAST(:port AS TEXT) DESC NULLS LAST,
              left(v.cfr, 3) = CAST(:flag AS TEXT) DESC NULLS LAST,
              abs(v.id - :id), v.id
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findRelatedWithApprovedPhotos(
        id: Int,
        port: String?,
        flag: String?,
        mid: String?,
        limit: Int,
    ): List<VesselEntity>
}

interface DBPhotoRepository : JpaRepository<PhotoEntity, Int> {
    fun findByStatusOrderByCreatedAt(status: PhotoStatus): List<PhotoEntity>

    fun findByVesselIdInAndStatusOrderByNumber(
        vesselIds: Collection<Int>,
        status: PhotoStatus,
    ): List<PhotoEntity>

    fun findByVesselIdAndNumberAndStatus(
        vesselId: Int,
        number: Int,
        status: PhotoStatus,
    ): PhotoEntity?

    @Query("SELECT coalesce(max(p.number), 0) + 1 FROM PhotoEntity p WHERE p.vesselId = :vesselId")
    fun nextNumber(vesselId: Int): Int

    fun countByStatus(status: PhotoStatus): Long

    fun findByApiKeyIdAndContributorIdAndStatusInAndSeenAtIsNullOrderByReviewedAt(
        apiKeyId: Int,
        contributorId: String,
        statuses: Collection<PhotoStatus>,
    ): List<PhotoEntity>

    fun existsBySourceUrl(sourceUrl: String): Boolean
}

interface DBPhotoFileRepository : JpaRepository<PhotoFileEntity, PhotoFileId> {
    @Query("SELECT f.content FROM PhotoFileEntity f WHERE f.id.photoId = :photoId AND f.id.size = :size")
    fun findContent(
        photoId: Int,
        size: String,
    ): ByteArray?

    @Modifying
    @Query("DELETE FROM PhotoFileEntity f WHERE f.id.photoId = :photoId")
    fun deleteByPhotoId(photoId: Int)
}

interface DBKeyLinkRequestRepository : JpaRepository<KeyLinkRequestEntity, Int> {
    fun findByStatusOrderByCreatedAt(status: ReviewStatus): List<KeyLinkRequestEntity>

    fun findByVesselIdAndStatus(
        vesselId: Int,
        status: ReviewStatus,
    ): List<KeyLinkRequestEntity>
}

interface DBReportRepository : JpaRepository<ReportEntity, Int> {
    fun findByStatusOrderByCreatedAt(status: ReportStatus): List<ReportEntity>

    fun findByPhotoIdAndStatus(
        photoId: Int,
        status: ReportStatus,
    ): List<ReportEntity>
}

interface DBApiKeyRepository : JpaRepository<ApiKeyEntity, Int> {
    fun findByKeyHash(keyHash: String): ApiKeyEntity?

    fun findByStatusOrderByCreatedAt(status: ApiKeyStatus): List<ApiKeyEntity>

    @Modifying
    @Query("UPDATE ApiKeyEntity k SET k.lastUsedAt = CURRENT_TIMESTAMP WHERE k.id = :id")
    fun markUsed(id: Int)
}
