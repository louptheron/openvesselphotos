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

    @Query(
        value = """
            SELECT count(*) FROM vessels v
            WHERE EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED')
        """,
        nativeQuery = true,
    )
    fun countWithApprovedPhotos(): Long

    @Query(
        value = """
            SELECT * FROM vessels v
            WHERE v.port_code = :code
              AND EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED') = :isPhotographed
            ORDER BY v.name NULLS LAST, v.id
            OFFSET :offset LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findByPortCode(
        code: String,
        isPhotographed: Boolean,
        offset: Int,
        limit: Int,
    ): List<VesselEntity>

    @Query(
        value = """
            SELECT * FROM vessels v
            WHERE EXISTS (SELECT 1 FROM photos p WHERE p.vessel_id = v.id AND p.status = 'APPROVED')
            ORDER BY v.id
            OFFSET :offset LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findPageWithApprovedPhotos(
        offset: Int,
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
