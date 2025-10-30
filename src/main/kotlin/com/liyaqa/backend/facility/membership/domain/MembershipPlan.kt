package com.liyaqa.backend.facility.membership.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.facility.domain.SportFacility
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal

/**
 * Membership plan offered by a branch.
 *
 * Membership plans define different tiers of membership that customers can purchase
 * (e.g., Basic, Premium, Family, Student, Corporate).
 *
 * Key Features:
 * - Defined at branch level (each branch can have different plans)
 * - Pricing and duration configuration
 * - Benefits and features description
 * - Access level control
 * - Active/inactive status
 */
@Entity
@Table(
    name = "membership_plans",
    indexes = [
        Index(name = "idx_membership_plan_branch", columnList = "branch_id"),
        Index(name = "idx_membership_plan_facility", columnList = "facility_id"),
        Index(name = "idx_membership_plan_name", columnList = "name"),
        Index(name = "idx_membership_plan_status", columnList = "is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_branch_plan_name", columnNames = ["branch_id", "name"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class MembershipPlan(
    // Branch this plan belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    var branch: FacilityBranch,

    // Facility (for easier querying)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    // === Basic Information ===
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "plan_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    var planType: MembershipPlanType = MembershipPlanType.INDIVIDUAL,

    // === Pricing ===
    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal,

    @Column(length = 3, nullable = false)
    var currency: String = "USD",

    @Column(name = "billing_cycle", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    var billingCycle: BillingCycle = BillingCycle.MONTHLY,

    // === Duration ===
    @Column(name = "duration_months", nullable = false)
    var durationMonths: Int, // e.g., 1 for monthly, 12 for yearly

    // === Features & Benefits ===
    @Column(columnDefinition = "TEXT")
    var features: String? = null, // JSON or comma-separated list

    @Column(name = "max_bookings_per_month")
    var maxBookingsPerMonth: Int? = null, // null = unlimited

    @Column(name = "max_concurrent_bookings")
    var maxConcurrentBookings: Int? = null, // null = unlimited

    @Column(name = "advance_booking_days")
    var advanceBookingDays: Int? = null, // How many days in advance can book

    @Column(name = "cancellation_hours")
    var cancellationHours: Int? = null, // How many hours before can cancel

    @Column(name = "guest_passes")
    var guestPasses: Int? = null, // Number of guest passes per month

    // === Access Control ===
    @Column(name = "has_court_access")
    var hasCourtAccess: Boolean = true,

    @Column(name = "has_class_access")
    var hasClassAccess: Boolean = false,

    @Column(name = "has_gym_access")
    var hasGymAccess: Boolean = false,

    @Column(name = "has_locker_access")
    var hasLockerAccess: Boolean = false,

    @Column(name = "priority_level")
    var priorityLevel: Int = 0, // Higher number = higher priority for bookings

    // === Discounts ===
    @Column(name = "setup_fee", precision = 10, scale = 2)
    var setupFee: BigDecimal? = null,

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    var discountPercentage: BigDecimal? = null, // For promotional pricing

    // === Status ===
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "is_visible", nullable = false)
    var isVisible: Boolean = true, // Show on website/app

    @Column(name = "max_members")
    var maxMembers: Int? = null, // Capacity limit

    @Column(name = "current_members")
    var currentMembers: Int = 0,

    // === Terms ===
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    var termsAndConditions: String? = null,

    @Column(name = "auto_renew")
    var autoRenew: Boolean = true

) : BaseEntity() {

    // Multi-tenancy field
    @Column(name = "tenant_id", length = 100, nullable = false)
    override lateinit var tenantId: String

    /**
     * Check if plan is at capacity.
     */
    fun isAtCapacity(): Boolean {
        return maxMembers?.let { currentMembers >= it } ?: false
    }

    /**
     * Check if plan is available for new members.
     */
    fun isAvailable(): Boolean {
        return isActive && isVisible && !isAtCapacity()
    }

    /**
     * Calculate effective price after discount.
     */
    fun getEffectivePrice(): BigDecimal {
        return discountPercentage?.let {
            val discount = price.multiply(it).divide(BigDecimal.valueOf(100))
            price.subtract(discount)
        } ?: price
    }

    /**
     * Get total first payment (price + setup fee).
     */
    fun getFirstPaymentAmount(): BigDecimal {
        val basePrice = getEffectivePrice()
        return setupFee?.let { basePrice.add(it) } ?: basePrice
    }

    /**
     * Increment member count.
     */
    fun incrementMemberCount() {
        currentMembers++
    }

    /**
     * Decrement member count.
     */
    fun decrementMemberCount() {
        if (currentMembers > 0) {
            currentMembers--
        }
    }

    override fun toString(): String {
        return "MembershipPlan(id=$id, name='$name', branch=${branch.name}, price=$price $currency/${billingCycle.name})"
    }
}

/**
 * Type of membership plan.
 */
enum class MembershipPlanType {
    INDIVIDUAL,  // Single person
    FAMILY,      // Family package (multiple members)
    STUDENT,     // Student discount
    SENIOR,      // Senior citizen discount
    CORPORATE,   // Corporate/group membership
    TRIAL,       // Trial period
    DAY_PASS,    // Single day access
    PUNCH_CARD   // Pay per use (e.g., 10 visits)
}

/**
 * Billing cycle for membership.
 */
enum class BillingCycle {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUAL,
    ANNUAL,
    ONE_TIME
}
