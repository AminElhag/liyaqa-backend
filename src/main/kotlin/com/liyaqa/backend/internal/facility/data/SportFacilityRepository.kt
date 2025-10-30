package com.liyaqa.backend.internal.facility.data

import com.liyaqa.backend.internal.facility.domain.FacilityStatus
import com.liyaqa.backend.internal.facility.domain.SportFacility
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for SportFacility entity with comprehensive query capabilities.
 *
 * Supports searching, filtering, and analytics for sport facilities.
 */
@Repository
interface SportFacilityRepository : JpaRepository<SportFacility, UUID> {

    /**
     * Find facilities by owning tenant.
     */
    @Query("""
        SELECT f FROM SportFacility f
        WHERE f.owner.id = :tenantId
        ORDER BY f.createdAt DESC
    """)
    fun findByOwnerTenantId(@Param("tenantId") tenantId: UUID): List<SportFacility>

    /**
     * Find facilities by tenant ID string (for multi-tenancy).
     */
    fun findByTenantId(tenantId: String): List<SportFacility>

    /**
     * Find facilities by status.
     */
    fun findByStatus(status: FacilityStatus): List<SportFacility>

    /**
     * Count facilities by status.
     */
    fun countByStatus(status: FacilityStatus): Long

    /**
     * Find facilities by facility type.
     */
    fun findByFacilityType(facilityType: String): List<SportFacility>

    /**
     * Count facilities by facility type.
     */
    fun countByFacilityType(facilityType: String): Long

    /**
     * Search facilities with multiple filters.
     */
    @Query("""
        SELECT f FROM SportFacility f
        WHERE (:searchTerm IS NULL
            OR LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(f.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:status IS NULL OR f.status = :status)
        AND (:facilityType IS NULL OR f.facilityType = :facilityType)
        AND (:ownerTenantId IS NULL OR f.owner.id = :ownerTenantId)
        ORDER BY f.createdAt DESC
    """)
    fun searchFacilities(
        @Param("searchTerm") searchTerm: String?,
        @Param("status") status: FacilityStatus?,
        @Param("facilityType") facilityType: String?,
        @Param("ownerTenantId") ownerTenantId: UUID?,
        pageable: Pageable
    ): Page<SportFacility>

    /**
     * Find active facilities by owner.
     */
    @Query("""
        SELECT f FROM SportFacility f
        WHERE f.owner.id = :tenantId
        AND f.status = 'ACTIVE'
        ORDER BY f.name ASC
    """)
    fun findActiveByOwner(@Param("tenantId") tenantId: UUID): List<SportFacility>

    /**
     * Count facilities by owner tenant.
     */
    @Query("""
        SELECT COUNT(f) FROM SportFacility f
        WHERE f.owner.id = :tenantId
    """)
    fun countByOwnerTenantId(@Param("tenantId") tenantId: UUID): Long

    /**
     * Find facilities by contact email (for contact lookups).
     */
    fun findByContactEmail(email: String): List<SportFacility>

    /**
     * Check if facility name exists for tenant.
     */
    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END
        FROM SportFacility f
        WHERE f.owner.id = :tenantId
        AND LOWER(f.name) = LOWER(:name)
    """)
    fun existsByOwnerAndName(
        @Param("tenantId") tenantId: UUID,
        @Param("name") name: String
    ): Boolean

    /**
     * Get facility statistics grouped by type.
     */
    @Query("""
        SELECT f.facilityType, COUNT(f)
        FROM SportFacility f
        WHERE f.status = 'ACTIVE'
        GROUP BY f.facilityType
        ORDER BY COUNT(f) DESC
    """)
    fun countActiveByFacilityTypeGrouped(): List<Array<Any>>

    /**
     * Get facility statistics grouped by status.
     */
    @Query("""
        SELECT f.status, COUNT(f)
        FROM SportFacility f
        GROUP BY f.status
    """)
    fun countByStatusGrouped(): List<Array<Any>>

    /**
     * Find recently created facilities.
     */
    @Query("""
        SELECT f FROM SportFacility f
        WHERE f.status = 'ACTIVE'
        ORDER BY f.createdAt DESC
    """)
    fun findRecentlyCreated(pageable: Pageable): Page<SportFacility>

    /**
     * Find facilities needing attention (inactive, under maintenance).
     */
    @Query("""
        SELECT f FROM SportFacility f
        WHERE f.status IN ('INACTIVE', 'UNDER_MAINTENANCE')
        ORDER BY f.updatedAt DESC
    """)
    fun findNeedingAttention(): List<SportFacility>
}
