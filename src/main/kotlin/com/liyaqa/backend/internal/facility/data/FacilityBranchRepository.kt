package com.liyaqa.backend.internal.facility.data

import com.liyaqa.backend.internal.facility.domain.BranchStatus
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.*

/**
 * Repository for FacilityBranch entity with comprehensive query capabilities.
 *
 * Supports searching, filtering, and geographic queries for facility branches.
 */
@Repository
interface FacilityBranchRepository : JpaRepository<FacilityBranch, UUID> {

    /**
     * Find branches by facility.
     */
    @Query("""
        SELECT b FROM FacilityBranch b
        WHERE b.facility.id = :facilityId
        ORDER BY b.isMainBranch DESC, b.name ASC
    """)
    fun findByFacilityId(@Param("facilityId") facilityId: UUID): List<FacilityBranch>

    /**
     * Find branches by tenant ID string (for multi-tenancy).
     */
    fun findByTenantId(tenantId: String): List<FacilityBranch>

    /**
     * Find main branch for facility.
     */
    @Query("""
        SELECT b FROM FacilityBranch b
        WHERE b.facility.id = :facilityId
        AND b.isMainBranch = TRUE
    """)
    fun findMainBranchByFacilityId(@Param("facilityId") facilityId: UUID): FacilityBranch?

    /**
     * Find branches by status.
     */
    fun findByStatus(status: BranchStatus): List<FacilityBranch>

    /**
     * Count branches by status.
     */
    fun countByStatus(status: BranchStatus): Long

    /**
     * Find branches by city.
     */
    fun findByCity(city: String): List<FacilityBranch>

    /**
     * Find branches by country.
     */
    fun findByCountry(country: String): List<FacilityBranch>

    /**
     * Search branches with multiple filters.
     */
    @Query("""
        SELECT b FROM FacilityBranch b
        WHERE (:searchTerm IS NULL
            OR LOWER(b.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(b.city) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(b.addressLine1) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:status IS NULL OR b.status = :status)
        AND (:facilityId IS NULL OR b.facility.id = :facilityId)
        AND (:city IS NULL OR LOWER(b.city) = LOWER(:city))
        AND (:country IS NULL OR LOWER(b.country) = LOWER(:country))
        ORDER BY b.isMainBranch DESC, b.createdAt DESC
    """)
    fun searchBranches(
        @Param("searchTerm") searchTerm: String?,
        @Param("status") status: BranchStatus?,
        @Param("facilityId") facilityId: UUID?,
        @Param("city") city: String?,
        @Param("country") country: String?,
        pageable: Pageable
    ): Page<FacilityBranch>

    /**
     * Find active branches by facility.
     */
    @Query("""
        SELECT b FROM FacilityBranch b
        WHERE b.facility.id = :facilityId
        AND b.status = 'ACTIVE'
        ORDER BY b.isMainBranch DESC, b.name ASC
    """)
    fun findActiveByFacility(@Param("facilityId") facilityId: UUID): List<FacilityBranch>

    /**
     * Count branches by facility.
     */
    @Query("""
        SELECT COUNT(b) FROM FacilityBranch b
        WHERE b.facility.id = :facilityId
    """)
    fun countByFacilityId(@Param("facilityId") facilityId: UUID): Long

    /**
     * Count active branches by facility.
     */
    @Query("""
        SELECT COUNT(b) FROM FacilityBranch b
        WHERE b.facility.id = :facilityId
        AND b.status = 'ACTIVE'
    """)
    fun countActiveByFacilityId(@Param("facilityId") facilityId: UUID): Long

    /**
     * Find branches near geographic coordinates (simplified distance calculation).
     * Note: This is a simple rectangular search, not precise distance.
     * For production, consider using PostGIS or similar.
     */
    @Query("""
        SELECT b FROM FacilityBranch b
        WHERE b.latitude IS NOT NULL
        AND b.longitude IS NOT NULL
        AND b.latitude BETWEEN :minLat AND :maxLat
        AND b.longitude BETWEEN :minLon AND :maxLon
        AND b.status = 'ACTIVE'
        ORDER BY b.name ASC
    """)
    fun findNearCoordinates(
        @Param("minLat") minLat: BigDecimal,
        @Param("maxLat") maxLat: BigDecimal,
        @Param("minLon") minLon: BigDecimal,
        @Param("maxLon") maxLon: BigDecimal
    ): List<FacilityBranch>

    /**
     * Find branches with coordinates (for mapping).
     */
    @Query("""
        SELECT b FROM FacilityBranch b
        WHERE b.latitude IS NOT NULL
        AND b.longitude IS NOT NULL
        AND b.status = 'ACTIVE'
        ORDER BY b.name ASC
    """)
    fun findWithCoordinates(): List<FacilityBranch>

    /**
     * Check if branch name exists for facility.
     */
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END
        FROM FacilityBranch b
        WHERE b.facility.id = :facilityId
        AND LOWER(b.name) = LOWER(:name)
    """)
    fun existsByFacilityAndName(
        @Param("facilityId") facilityId: UUID,
        @Param("name") name: String
    ): Boolean

    /**
     * Get branch statistics grouped by status.
     */
    @Query("""
        SELECT b.status, COUNT(b)
        FROM FacilityBranch b
        GROUP BY b.status
    """)
    fun countByStatusGrouped(): List<Array<Any>>

    /**
     * Get branch statistics grouped by city.
     */
    @Query("""
        SELECT b.city, COUNT(b)
        FROM FacilityBranch b
        WHERE b.status = 'ACTIVE'
        GROUP BY b.city
        ORDER BY COUNT(b) DESC
    """)
    fun countActiveByCityGrouped(): List<Array<Any>>

    /**
     * Find branches needing attention (inactive, under renovation, etc.).
     */
    @Query("""
        SELECT b FROM FacilityBranch b
        WHERE b.status IN ('INACTIVE', 'UNDER_RENOVATION', 'TEMPORARILY_CLOSED')
        ORDER BY b.updatedAt DESC
    """)
    fun findNeedingAttention(): List<FacilityBranch>

    /**
     * Get total capacity across all active branches for a facility.
     */
    @Query("""
        SELECT SUM(b.totalCourts), SUM(b.totalCapacity)
        FROM FacilityBranch b
        WHERE b.facility.id = :facilityId
        AND b.status = 'ACTIVE'
    """)
    fun getTotalCapacityByFacility(@Param("facilityId") facilityId: UUID): Array<Any>?
}
