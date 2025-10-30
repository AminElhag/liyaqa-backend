package com.liyaqa.backend.facility.membership.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.facility.domain.SportFacility
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Membership - links a member to a membership plan with dates and payment info.
 *
 * Represents an active or historical membership subscription.
 *
 * Key Features:
 * - Links member to plan
 * - Tracks start/end dates
 * - Payment and billing information
 * - Status management (active, expired, cancelled, suspended)
 * - Auto-renewal tracking
 * - Usage tracking
 */
@Entity
@Table(
    name = "memberships",
    indexes = [
        Index(name = "idx_membership_member", columnList = "member_id"),
        Index(name = "idx_membership_plan", columnList = "plan_id"),
        Index(name = "idx_membership_branch", columnList = "branch_id"),
        Index(name = "idx_membership_facility", columnList = "facility_id"),
        Index(name = "idx_membership_status", columnList = "status"),
        Index(name = "idx_membership_start_date", columnList = "start_date"),
        Index(name = "idx_membership_end_date", columnList = "end_date"),
        Index(name = "idx_membership_number", columnList = "membership_number")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_membership_number", columnNames = ["membership_number"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Membership(
    // Member who owns this membership
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    // Plan this membership is based on
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    var plan: MembershipPlan,

    // Branch where membership is registered
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    var branch: FacilityBranch,

    // Facility (for easier querying)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    // === Membership Details ===
    @Column(name = "membership_number", length = 50, unique = true, nullable = false)
    var membershipNumber: String,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    // === Status ===
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: MembershipStatus = MembershipStatus.ACTIVE,

    @Column(name = "status_changed_at")
    var statusChangedAt: Instant? = null,

    @Column(name = "status_reason", columnDefinition = "TEXT")
    var statusReason: String? = null,

    // === Pricing & Payment ===
    @Column(name = "price_paid", precision = 10, scale = 2, nullable = false)
    var pricePaid: BigDecimal,

    @Column(name = "setup_fee_paid", precision = 10, scale = 2)
    var setupFeePaid: BigDecimal? = null,

    @Column(length = 3, nullable = false)
    var currency: String,

    @Column(name = "payment_method", length = 50)
    var paymentMethod: String? = null, // CARD, CASH, BANK_TRANSFER, etc.

    @Column(name = "payment_reference", length = 255)
    var paymentReference: String? = null,

    @Column(name = "paid_at")
    var paidAt: Instant? = null,

    // === Auto-Renewal ===
    @Column(name = "auto_renew")
    var autoRenew: Boolean = false,

    @Column(name = "next_billing_date")
    var nextBillingDate: LocalDate? = null,

    @Column(name = "renewal_reminder_sent")
    var renewalReminderSent: Boolean = false,

    // === Usage Tracking ===
    @Column(name = "bookings_used")
    var bookingsUsed: Int = 0,

    @Column(name = "guest_passes_used")
    var guestPassesUsed: Int = 0,

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,

    // === Cancellation ===
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,

    @Column(name = "cancelled_by", length = 255)
    var cancelledBy: String? = null,

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    var cancellationReason: String? = null,

    // === Notes ===
    @Column(columnDefinition = "TEXT")
    var notes: String? = null

) : BaseEntity() {

    // Multi-tenancy field
    @Column(name = "tenant_id", length = 100, nullable = false)
    override lateinit var tenantId: String

    /**
     * Check if membership is currently active and valid.
     */
    fun isCurrentlyActive(): Boolean {
        if (status != MembershipStatus.ACTIVE) return false

        val today = LocalDate.now()
        return !today.isBefore(startDate) && !today.isAfter(endDate)
    }

    /**
     * Check if membership has expired.
     */
    fun isExpired(): Boolean {
        return LocalDate.now().isAfter(endDate)
    }

    /**
     * Check if membership is about to expire (within days).
     */
    fun isExpiringWithin(days: Int): Boolean {
        val today = LocalDate.now()
        val expiryThreshold = today.plusDays(days.toLong())
        return today.isBefore(endDate) && endDate.isBefore(expiryThreshold)
    }

    /**
     * Check if can make booking (considering plan limits).
     */
    fun canMakeBooking(): Boolean {
        if (!isCurrentlyActive()) return false

        plan.maxBookingsPerMonth?.let { limit ->
            if (bookingsUsed >= limit) return false
        }

        return true
    }

    /**
     * Increment booking usage.
     */
    fun recordBooking() {
        bookingsUsed++
        lastUsedAt = Instant.now()
    }

    /**
     * Use guest pass.
     */
    fun useGuestPass(): Boolean {
        plan.guestPasses?.let { available ->
            if (guestPassesUsed < available) {
                guestPassesUsed++
                return true
            }
        }
        return false
    }

    /**
     * Cancel membership.
     */
    fun cancel(reason: String, cancelledBy: String) {
        this.status = MembershipStatus.CANCELLED
        this.statusChangedAt = Instant.now()
        this.statusReason = reason
        this.cancelledAt = Instant.now()
        this.cancelledBy = cancelledBy
        this.cancellationReason = reason
        this.autoRenew = false
    }

    /**
     * Suspend membership.
     */
    fun suspend(reason: String) {
        this.status = MembershipStatus.SUSPENDED
        this.statusChangedAt = Instant.now()
        this.statusReason = reason
    }

    /**
     * Reactivate suspended membership.
     */
    fun reactivate() {
        if (status == MembershipStatus.SUSPENDED && !isExpired()) {
            this.status = MembershipStatus.ACTIVE
            this.statusChangedAt = Instant.now()
            this.statusReason = null
        }
    }

    /**
     * Mark as expired.
     */
    fun markExpired() {
        if (isExpired() && status == MembershipStatus.ACTIVE) {
            this.status = MembershipStatus.EXPIRED
            this.statusChangedAt = Instant.now()
        }
    }

    /**
     * Renew membership.
     */
    fun renew(newEndDate: LocalDate, pricePaid: BigDecimal) {
        this.endDate = newEndDate
        this.pricePaid = pricePaid
        this.paidAt = Instant.now()
        this.status = MembershipStatus.ACTIVE
        this.statusChangedAt = Instant.now()
        this.bookingsUsed = 0
        this.guestPassesUsed = 0
        this.renewalReminderSent = false

        if (autoRenew) {
            this.nextBillingDate = newEndDate
        }
    }

    override fun toString(): String {
        return "Membership(id=$id, number='$membershipNumber', member=${member.getFullName()}, plan=${plan.name}, status=$status, $startDate to $endDate)"
    }
}

/**
 * Membership status enum.
 */
enum class MembershipStatus {
    ACTIVE,      // Currently active and valid
    EXPIRED,     // Past end date
    CANCELLED,   // Cancelled before expiry
    SUSPENDED,   // Temporarily suspended
    PENDING      // Payment pending or awaiting activation
}
