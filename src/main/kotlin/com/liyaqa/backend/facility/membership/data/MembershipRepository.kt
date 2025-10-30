package com.liyaqa.backend.facility.membership.data

import com.liyaqa.backend.facility.membership.domain.Membership
import com.liyaqa.backend.facility.membership.domain.MembershipStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

/**
 * Repository for Membership entity with comprehensive query capabilities.
 */
@Repository
interface MembershipRepository : JpaRepository<Membership, UUID> {

    /**
     * Find membership by membership number.
     */
    fun findByMembershipNumber(membershipNumber: String): Membership?

    /**
     * Find memberships by member.
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.member.id = :memberId
        ORDER BY m.startDate DESC
    """)
    fun findByMemberId(@Param("memberId") memberId: UUID): List<Membership>

    /**
     * Find active membership for member.
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.member.id = :memberId
        AND m.status = 'ACTIVE'
        AND m.startDate <= CURRENT_DATE
        AND m.endDate >= CURRENT_DATE
        ORDER BY m.endDate DESC
    """)
    fun findActiveMembershipByMemberId(@Param("memberId") memberId: UUID): Membership?

    /**
     * Find memberships by plan.
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.plan.id = :planId
        ORDER BY m.startDate DESC
    """)
    fun findByPlanId(@Param("planId") planId: UUID): List<Membership>

    /**
     * Find memberships by branch.
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.branch.id = :branchId
        ORDER BY m.startDate DESC
    """)
    fun findByBranchId(@Param("branchId") branchId: UUID): List<Membership>

    /**
     * Find active memberships by branch.
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.branch.id = :branchId
        AND m.status = 'ACTIVE'
        ORDER BY m.startDate DESC
    """)
    fun findActiveByBranchId(@Param("branchId") branchId: UUID): List<Membership>

    /**
     * Find memberships by facility.
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.facility.id = :facilityId
        ORDER BY m.startDate DESC
    """)
    fun findByFacilityId(@Param("facilityId") facilityId: UUID): List<Membership>

    /**
     * Find expiring memberships (within days).
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.status = 'ACTIVE'
        AND m.endDate BETWEEN CURRENT_DATE AND :expiryDate
        ORDER BY m.endDate ASC
    """)
    fun findExpiringBefore(@Param("expiryDate") expiryDate: LocalDate): List<Membership>

    /**
     * Find expired active memberships (that need status update).
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.status = 'ACTIVE'
        AND m.endDate < CURRENT_DATE
        ORDER BY m.endDate ASC
    """)
    fun findExpiredActive(): List<Membership>

    /**
     * Search memberships with filters.
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE (:searchTerm IS NULL
            OR LOWER(m.membershipNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.member.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.member.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(m.member.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND (:status IS NULL OR m.status = :status)
        AND (:branchId IS NULL OR m.branch.id = :branchId)
        AND (:facilityId IS NULL OR m.facility.id = :facilityId)
        ORDER BY m.startDate DESC
    """)
    fun searchMemberships(
        @Param("searchTerm") searchTerm: String?,
        @Param("status") status: MembershipStatus?,
        @Param("branchId") branchId: UUID?,
        @Param("facilityId") facilityId: UUID?,
        pageable: Pageable
    ): Page<Membership>

    /**
     * Count memberships by status.
     */
    fun countByStatus(status: MembershipStatus): Long

    /**
     * Count memberships by branch.
     */
    @Query("""
        SELECT COUNT(m) FROM Membership m
        WHERE m.branch.id = :branchId
    """)
    fun countByBranchId(@Param("branchId") branchId: UUID): Long

    /**
     * Count active memberships by branch.
     */
    @Query("""
        SELECT COUNT(m) FROM Membership m
        WHERE m.branch.id = :branchId
        AND m.status = 'ACTIVE'
    """)
    fun countActiveByBranchId(@Param("branchId") branchId: UUID): Long

    /**
     * Count active memberships by plan.
     */
    @Query("""
        SELECT COUNT(m) FROM Membership m
        WHERE m.plan.id = :planId
        AND m.status = 'ACTIVE'
    """)
    fun countActiveByPlanId(@Param("planId") planId: UUID): Long

    /**
     * Find memberships needing renewal reminder.
     */
    @Query("""
        SELECT m FROM Membership m
        WHERE m.status = 'ACTIVE'
        AND m.autoRenew = true
        AND m.renewalReminderSent = false
        AND m.endDate BETWEEN CURRENT_DATE AND :reminderDate
        ORDER BY m.endDate ASC
    """)
    fun findNeedingRenewalReminder(@Param("reminderDate") reminderDate: LocalDate): List<Membership>

    /**
     * Find memberships by tenant ID.
     */
    fun findByTenantId(tenantId: String): List<Membership>
}
