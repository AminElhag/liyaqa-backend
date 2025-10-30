package com.liyaqa.backend.facility.membership.data

import com.liyaqa.backend.facility.membership.domain.MembershipPlan
import com.liyaqa.backend.facility.membership.domain.MembershipPlanType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for MembershipPlan entity.
 */
@Repository
interface MembershipPlanRepository : JpaRepository<MembershipPlan, UUID> {

    /**
     * Find plans by branch.
     */
    @Query("""
        SELECT p FROM MembershipPlan p
        WHERE p.branch.id = :branchId
        ORDER BY p.price ASC
    """)
    fun findByBranchId(@Param("branchId") branchId: UUID): List<MembershipPlan>

    /**
     * Find active plans by branch.
     */
    @Query("""
        SELECT p FROM MembershipPlan p
        WHERE p.branch.id = :branchId
        AND p.isActive = true
        ORDER BY p.price ASC
    """)
    fun findActiveByBranchId(@Param("branchId") branchId: UUID): List<MembershipPlan>

    /**
     * Find visible plans by branch (for public display).
     */
    @Query("""
        SELECT p FROM MembershipPlan p
        WHERE p.branch.id = :branchId
        AND p.isActive = true
        AND p.isVisible = true
        ORDER BY p.price ASC
    """)
    fun findVisibleByBranchId(@Param("branchId") branchId: UUID): List<MembershipPlan>

    /**
     * Find plans by facility.
     */
    @Query("""
        SELECT p FROM MembershipPlan p
        WHERE p.facility.id = :facilityId
        ORDER BY p.branch.name ASC, p.price ASC
    """)
    fun findByFacilityId(@Param("facilityId") facilityId: UUID): List<MembershipPlan>

    /**
     * Find plans by type.
     */
    fun findByPlanType(planType: MembershipPlanType): List<MembershipPlan>

    /**
     * Check if plan name exists for branch.
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
        FROM MembershipPlan p
        WHERE p.branch.id = :branchId
        AND p.name = :name
    """)
    fun existsByBranchIdAndName(
        @Param("branchId") branchId: UUID,
        @Param("name") name: String
    ): Boolean

    /**
     * Count plans by branch.
     */
    @Query("""
        SELECT COUNT(p) FROM MembershipPlan p
        WHERE p.branch.id = :branchId
    """)
    fun countByBranchId(@Param("branchId") branchId: UUID): Long

    /**
     * Find plans by tenant ID.
     */
    fun findByTenantId(tenantId: String): List<MembershipPlan>
}
