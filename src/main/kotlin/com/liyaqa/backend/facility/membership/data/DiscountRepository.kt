package com.liyaqa.backend.facility.membership.data

import com.liyaqa.backend.facility.membership.domain.Discount
import com.liyaqa.backend.facility.membership.domain.DiscountApplicationMethod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

/**
 * Repository for managing discounts.
 */
@Repository
interface DiscountRepository : JpaRepository<Discount, UUID> {

    /**
     * Find discount by code and facility.
     */
    fun findByCodeAndFacilityId(code: String, facilityId: UUID): Discount?

    /**
     * Find discount by code, facility, and ensure it's active and valid.
     */
    @Query("""
        SELECT d FROM Discount d
        WHERE d.code = :code
        AND d.facility.id = :facilityId
        AND d.isActive = true
        AND :currentDate BETWEEN d.validFrom AND d.validUntil
    """)
    fun findValidDiscountByCode(
        @Param("code") code: String,
        @Param("facilityId") facilityId: UUID,
        @Param("currentDate") currentDate: LocalDate = LocalDate.now()
    ): Discount?

    /**
     * Find all discounts for a facility.
     */
    fun findByFacilityId(facilityId: UUID): List<Discount>

    /**
     * Find active discounts for a facility.
     */
    fun findByFacilityIdAndIsActive(facilityId: UUID, isActive: Boolean): List<Discount>

    /**
     * Find all discounts for a branch.
     */
    fun findByBranchId(branchId: UUID): List<Discount>

    /**
     * Find active discounts for a branch.
     */
    fun findByBranchIdAndIsActive(branchId: UUID, isActive: Boolean): List<Discount>

    /**
     * Find discounts by application method.
     */
    fun findByFacilityIdAndApplicationMethod(
        facilityId: UUID,
        applicationMethod: DiscountApplicationMethod
    ): List<Discount>

    /**
     * Find currently valid discounts (within date range and active).
     */
    @Query("""
        SELECT d FROM Discount d
        WHERE d.facility.id = :facilityId
        AND d.isActive = true
        AND :currentDate BETWEEN d.validFrom AND d.validUntil
    """)
    fun findCurrentlyValidDiscounts(
        @Param("facilityId") facilityId: UUID,
        @Param("currentDate") currentDate: LocalDate = LocalDate.now()
    ): List<Discount>

    /**
     * Find expiring discounts (expiring within days).
     */
    @Query("""
        SELECT d FROM Discount d
        WHERE d.facility.id = :facilityId
        AND d.isActive = true
        AND d.validUntil BETWEEN :currentDate AND :expiryDate
    """)
    fun findExpiringDiscounts(
        @Param("facilityId") facilityId: UUID,
        @Param("currentDate") currentDate: LocalDate,
        @Param("expiryDate") expiryDate: LocalDate
    ): List<Discount>

    /**
     * Find discounts that have reached their usage limit.
     */
    @Query("""
        SELECT d FROM Discount d
        WHERE d.facility.id = :facilityId
        AND d.maxTotalUsage IS NOT NULL
        AND d.currentUsageCount >= d.maxTotalUsage
    """)
    fun findDiscountsAtUsageLimit(@Param("facilityId") facilityId: UUID): List<Discount>

    /**
     * Check if a discount code exists for a facility.
     */
    fun existsByCodeAndFacilityId(code: String, facilityId: UUID): Boolean

    /**
     * Count discounts by facility.
     */
    fun countByFacilityId(facilityId: UUID): Long

    /**
     * Count active discounts by facility.
     */
    fun countByFacilityIdAndIsActive(facilityId: UUID, isActive: Boolean): Long
}
