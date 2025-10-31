package com.liyaqa.backend.facility.membership.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.facility.domain.SportFacility
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Discount types supported.
 */
enum class DiscountType {
    PERCENTAGE,     // Percentage off (e.g., 20%)
    FIXED_AMOUNT    // Fixed amount off (e.g., $50)
}

/**
 * How the discount is applied.
 */
enum class DiscountApplicationMethod {
    CODE,              // Customer enters promo code
    EMPLOYEE_APPLIED   // Employee applies discount manually
}

/**
 * Scope of discount applicability.
 */
enum class DiscountScope {
    ALL_PLANS,         // Applies to all membership plans
    SPECIFIC_PLANS,    // Applies to specific plans only
    SPECIFIC_TYPES     // Applies to specific plan types (e.g., only FAMILY plans)
}

/**
 * Represents a discount that can be applied to memberships.
 * Supports both promo codes and employee-applied discounts.
 */
@Entity
@Table(
    name = "discounts",
    indexes = [
        Index(name = "idx_discount_code", columnList = "code"),
        Index(name = "idx_discount_facility", columnList = "facility_id"),
        Index(name = "idx_discount_branch", columnList = "branch_id"),
        Index(name = "idx_discount_active", columnList = "is_active"),
        Index(name = "idx_discount_dates", columnList = "valid_from,valid_until"),
        Index(name = "idx_discount_tenant", columnList = "tenant_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_discount_code_facility", columnNames = ["code", "facility_id"])
    ]
)
class Discount(

    /**
     * Discount code (e.g., "SUMMER2025", "WELCOME10").
     * Can be null for employee-applied discounts.
     */
    @Column(length = 50)
    var code: String? = null,

    /**
     * Human-readable name/description.
     */
    @Column(nullable = false, length = 200)
    var name: String,

    /**
     * Detailed description of the discount.
     */
    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    /**
     * Discount type (percentage or fixed amount).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var discountType: DiscountType,

    /**
     * Discount value.
     * - For PERCENTAGE: value between 0-100 (e.g., 20 for 20%)
     * - For FIXED_AMOUNT: actual amount (e.g., 50.00 for $50)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    var value: BigDecimal,

    /**
     * Currency (for FIXED_AMOUNT discounts).
     */
    @Column(length = 3)
    var currency: String? = null,

    /**
     * How this discount is applied.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var applicationMethod: DiscountApplicationMethod,

    /**
     * Scope of applicability.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var scope: DiscountScope = DiscountScope.ALL_PLANS,

    /**
     * Facility this discount belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    /**
     * Branch this discount is specific to (optional).
     * If null, applies to all branches in the facility.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    var branch: FacilityBranch? = null,

    /**
     * Valid from date.
     */
    @Column(nullable = false)
    var validFrom: LocalDate,

    /**
     * Valid until date (inclusive).
     */
    @Column(nullable = false)
    var validUntil: LocalDate,

    /**
     * Is this discount currently active?
     */
    @Column(nullable = false)
    var isActive: Boolean = true,

    /**
     * Maximum number of times this discount can be used (total).
     * Null means unlimited.
     */
    @Column
    var maxTotalUsage: Int? = null,

    /**
     * Maximum number of times this discount can be used per member.
     * Null means unlimited per member.
     */
    @Column
    var maxUsagePerMember: Int? = null,

    /**
     * Current usage count.
     */
    @Column(nullable = false)
    var currentUsageCount: Int = 0,

    /**
     * Minimum membership price required to use this discount.
     */
    @Column(precision = 10, scale = 2)
    var minPurchaseAmount: BigDecimal? = null,

    /**
     * Maximum discount amount (cap for percentage discounts).
     */
    @Column(precision = 10, scale = 2)
    var maxDiscountAmount: BigDecimal? = null,

    /**
     * Internal notes about this discount (not visible to customers).
     */
    @Column(columnDefinition = "TEXT")
    var internalNotes: String? = null

) : BaseEntity() {

    // Multi-tenancy field
    @Column(name = "tenant_id", length = 100, nullable = false)
    override lateinit var tenantId: String

    /**
     * Membership plans this discount applies to (for SPECIFIC_PLANS scope).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "discount_applicable_plans",
        joinColumns = [JoinColumn(name = "discount_id")],
        inverseJoinColumns = [JoinColumn(name = "plan_id")]
    )
    var applicablePlans: MutableSet<MembershipPlan> = mutableSetOf()

    /**
     * Plan types this discount applies to (for SPECIFIC_TYPES scope).
     */
    @ElementCollection
    @CollectionTable(name = "discount_applicable_types", joinColumns = [JoinColumn(name = "discount_id")])
    @Column(name = "plan_type")
    @Enumerated(EnumType.STRING)
    var applicableTypes: MutableSet<MembershipPlanType> = mutableSetOf()

    /**
     * Check if discount is currently valid (within date range and active).
     */
    fun isCurrentlyValid(): Boolean {
        if (!isActive) return false
        val today = LocalDate.now()
        return !today.isBefore(validFrom) && !today.isAfter(validUntil)
    }

    /**
     * Check if discount has reached usage limit.
     */
    fun hasReachedUsageLimit(): Boolean {
        return maxTotalUsage?.let { currentUsageCount >= it } ?: false
    }

    /**
     * Check if discount can be applied to a specific plan.
     */
    fun isApplicableToPlan(plan: MembershipPlan): Boolean {
        return when (scope) {
            DiscountScope.ALL_PLANS -> true
            DiscountScope.SPECIFIC_PLANS -> applicablePlans.any { it.id == plan.id }
            DiscountScope.SPECIFIC_TYPES -> applicableTypes.contains(plan.planType)
        }
    }

    /**
     * Calculate discount amount for a given price.
     */
    fun calculateDiscountAmount(originalPrice: BigDecimal): BigDecimal {
        val calculatedDiscount = when (discountType) {
            DiscountType.PERCENTAGE -> originalPrice.multiply(value).divide(BigDecimal(100))
            DiscountType.FIXED_AMOUNT -> value
        }

        // Apply max discount cap if set
        return maxDiscountAmount?.let { cap ->
            if (calculatedDiscount > cap) cap else calculatedDiscount
        } ?: calculatedDiscount
    }

    /**
     * Calculate final price after discount.
     */
    fun calculateFinalPrice(originalPrice: BigDecimal): BigDecimal {
        val discountAmount = calculateDiscountAmount(originalPrice)
        val finalPrice = originalPrice.subtract(discountAmount)
        // Ensure price doesn't go below zero
        return if (finalPrice < BigDecimal.ZERO) BigDecimal.ZERO else finalPrice
    }

    /**
     * Increment usage count.
     */
    fun incrementUsage() {
        currentUsageCount++
    }

    /**
     * Validate minimum purchase requirement.
     */
    fun meetsMinimumPurchase(price: BigDecimal): Boolean {
        return minPurchaseAmount?.let { price >= it } ?: true
    }
}
