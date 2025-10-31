package com.liyaqa.backend.facility.membership.data

import com.liyaqa.backend.facility.membership.domain.DiscountUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Repository for tracking discount usage.
 */
@Repository
interface DiscountUsageRepository : JpaRepository<DiscountUsage, UUID> {

    /**
     * Find all usage records for a discount.
     */
    fun findByDiscountId(discountId: UUID): List<DiscountUsage>

    /**
     * Find all usage records for a member.
     */
    fun findByMemberId(memberId: UUID): List<DiscountUsage>

    /**
     * Find all usage records for a membership.
     */
    fun findByMembershipId(membershipId: UUID): List<DiscountUsage>

    /**
     * Find usage records by employee who applied the discount.
     */
    fun findByAppliedByEmployeeId(employeeId: UUID): List<DiscountUsage>

    /**
     * Count usage of a discount by a specific member.
     */
    fun countByDiscountIdAndMemberId(discountId: UUID, memberId: UUID): Long

    /**
     * Count total usage of a discount.
     */
    fun countByDiscountId(discountId: UUID): Long

    /**
     * Find usage within a date range for a facility.
     */
    @Query("""
        SELECT u FROM DiscountUsage u
        WHERE u.facility.id = :facilityId
        AND u.usedAt BETWEEN :startDate AND :endDate
        ORDER BY u.usedAt DESC
    """)
    fun findUsageInDateRange(
        @Param("facilityId") facilityId: UUID,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<DiscountUsage>

    /**
     * Calculate total savings from discounts for a facility.
     */
    @Query("""
        SELECT COALESCE(SUM(u.discountAmount), 0)
        FROM DiscountUsage u
        WHERE u.facility.id = :facilityId
        AND u.usedAt BETWEEN :startDate AND :endDate
    """)
    fun calculateTotalSavings(
        @Param("facilityId") facilityId: UUID,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): BigDecimal

    /**
     * Get top N most used discounts for a facility.
     */
    @Query("""
        SELECT u.discount.id, COUNT(u) as usageCount
        FROM DiscountUsage u
        WHERE u.facility.id = :facilityId
        GROUP BY u.discount.id
        ORDER BY usageCount DESC
    """)
    fun findTopUsedDiscounts(@Param("facilityId") facilityId: UUID): List<Array<Any>>

    /**
     * Find recent usage for a facility.
     */
    @Query("""
        SELECT u FROM DiscountUsage u
        WHERE u.facility.id = :facilityId
        ORDER BY u.usedAt DESC
    """)
    fun findRecentUsage(@Param("facilityId") facilityId: UUID): List<DiscountUsage>
}
