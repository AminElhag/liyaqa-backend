package com.liyaqa.backend.facility.booking.data

import com.liyaqa.backend.facility.booking.domain.Court
import com.liyaqa.backend.facility.booking.domain.CourtStatus
import com.liyaqa.backend.facility.booking.domain.CourtType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for Court entity.
 */
@Repository
interface CourtRepository : JpaRepository<Court, UUID> {

    /**
     * Find courts by facility.
     */
    @Query("""
        SELECT c FROM Court c
        WHERE c.facility.id = :facilityId
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    fun findByFacilityId(@Param("facilityId") facilityId: UUID): List<Court>

    /**
     * Find courts by branch.
     */
    @Query("""
        SELECT c FROM Court c
        WHERE c.branch.id = :branchId
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    fun findByBranchId(@Param("branchId") branchId: UUID): List<Court>

    /**
     * Find active courts by branch.
     */
    @Query("""
        SELECT c FROM Court c
        WHERE c.branch.id = :branchId
        AND c.status = 'ACTIVE'
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    fun findActiveByBranchId(@Param("branchId") branchId: UUID): List<Court>

    /**
     * Find courts by branch and type.
     */
    @Query("""
        SELECT c FROM Court c
        WHERE c.branch.id = :branchId
        AND c.courtType = :courtType
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    fun findByBranchIdAndCourtType(
        @Param("branchId") branchId: UUID,
        @Param("courtType") courtType: CourtType
    ): List<Court>

    /**
     * Find courts by branch and status.
     */
    @Query("""
        SELECT c FROM Court c
        WHERE c.branch.id = :branchId
        AND c.status = :status
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    fun findByBranchIdAndStatus(
        @Param("branchId") branchId: UUID,
        @Param("status") status: CourtStatus
    ): List<Court>

    /**
     * Check if court name exists for branch.
     */
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
        FROM Court c
        WHERE c.branch.id = :branchId
        AND c.name = :name
    """)
    fun existsByBranchIdAndName(
        @Param("branchId") branchId: UUID,
        @Param("name") name: String
    ): Boolean

    /**
     * Count courts by branch.
     */
    @Query("""
        SELECT COUNT(c) FROM Court c
        WHERE c.branch.id = :branchId
    """)
    fun countByBranchId(@Param("branchId") branchId: UUID): Long

    /**
     * Count active courts by branch.
     */
    @Query("""
        SELECT COUNT(c) FROM Court c
        WHERE c.branch.id = :branchId
        AND c.status = 'ACTIVE'
    """)
    fun countActiveByBranchId(@Param("branchId") branchId: UUID): Long

    /**
     * Find courts by type across facility.
     */
    @Query("""
        SELECT c FROM Court c
        WHERE c.facility.id = :facilityId
        AND c.courtType = :courtType
        AND c.status = 'ACTIVE'
        ORDER BY c.branch.name ASC, c.displayOrder ASC, c.name ASC
    """)
    fun findByFacilityIdAndCourtType(
        @Param("facilityId") facilityId: UUID,
        @Param("courtType") courtType: CourtType
    ): List<Court>

    /**
     * Find indoor courts by branch.
     */
    @Query("""
        SELECT c FROM Court c
        WHERE c.branch.id = :branchId
        AND c.isIndoor = true
        AND c.status = 'ACTIVE'
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    fun findIndoorByBranchId(@Param("branchId") branchId: UUID): List<Court>

    /**
     * Find courts with lighting by branch.
     */
    @Query("""
        SELECT c FROM Court c
        WHERE c.branch.id = :branchId
        AND c.hasLighting = true
        AND c.status = 'ACTIVE'
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    fun findWithLightingByBranchId(@Param("branchId") branchId: UUID): List<Court>
}
