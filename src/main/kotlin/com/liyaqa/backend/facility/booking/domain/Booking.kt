package com.liyaqa.backend.facility.booking.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.facility.membership.domain.Membership
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.facility.domain.SportFacility
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Booking/Reservation for a court at a specific date and time.
 */
@Entity
@Table(
    name = "bookings",
    indexes = [
        Index(name = "idx_booking_member", columnList = "member_id"),
        Index(name = "idx_booking_court", columnList = "court_id"),
        Index(name = "idx_booking_branch", columnList = "branch_id"),
        Index(name = "idx_booking_facility", columnList = "facility_id"),
        Index(name = "idx_booking_status", columnList = "status"),
        Index(name = "idx_booking_date", columnList = "booking_date"),
        Index(name = "idx_booking_start_time", columnList = "start_time"),
        Index(name = "idx_booking_number", columnList = "booking_number"),
        Index(name = "idx_booking_tenant", columnList = "tenant_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_booking_number", columnNames = ["booking_number"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Booking(
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    var court: Court,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    var branch: FacilityBranch,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id")
    var membership: Membership? = null, // Optional - if booked with membership benefits

    // Booking Details
    @Column(name = "booking_number", length = 50, nullable = false, unique = true)
    var bookingNumber: String,

    @Column(name = "booking_date", nullable = false)
    var bookingDate: LocalDate,

    @Column(name = "start_time", nullable = false)
    var startTime: LocalDateTime,

    @Column(name = "end_time", nullable = false)
    var endTime: LocalDateTime,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int,

    // Status
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: BookingStatus = BookingStatus.CONFIRMED,

    @Column(name = "status_reason", columnDefinition = "TEXT")
    var statusReason: String? = null,

    @Column(name = "status_changed_at")
    var statusChangedAt: Instant? = null,

    // Pricing
    @Column(name = "original_price", precision = 10, scale = 2, nullable = false)
    var originalPrice: BigDecimal,

    @Column(name = "discount_amount", precision = 10, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "final_price", precision = 10, scale = 2, nullable = false)
    var finalPrice: BigDecimal,

    @Column(length = 3, nullable = false)
    var currency: String = "USD",

    // Payment
    @Column(name = "payment_status", length = 50)
    @Enumerated(EnumType.STRING)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "payment_method", length = 50)
    var paymentMethod: String? = null,

    @Column(name = "payment_reference", length = 255)
    var paymentReference: String? = null,

    @Column(name = "paid_at")
    var paidAt: Instant? = null,

    // Participants
    @Column(name = "number_of_players")
    var numberOfPlayers: Int = 2,

    @Column(name = "additional_players", columnDefinition = "TEXT")
    var additionalPlayers: String? = null, // JSON array of player names/emails

    // Special Requests
    @Column(name = "special_requests", columnDefinition = "TEXT")
    var specialRequests: String? = null,

    // Cancellation
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,

    @Column(name = "cancelled_by", length = 255)
    var cancelledBy: String? = null,

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    var cancellationReason: String? = null,

    @Column(name = "refund_amount", precision = 10, scale = 2)
    var refundAmount: BigDecimal? = null,

    // Check-in/Check-out
    @Column(name = "checked_in_at")
    var checkedInAt: Instant? = null,

    @Column(name = "checked_out_at")
    var checkedOutAt: Instant? = null,

    // Notes
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    // Notifications
    @Column(name = "reminder_sent")
    var reminderSent: Boolean = false,

    @Column(name = "confirmation_sent")
    var confirmationSent: Boolean = false

) : BaseEntity() {

    // Multi-tenancy field
    @Column(name = "tenant_id", length = 100, nullable = false)
    override lateinit var tenantId: String

    /**
     * Check if booking is active (confirmed or checked-in).
     */
    fun isActive(): Boolean {
        return status == BookingStatus.CONFIRMED || status == BookingStatus.CHECKED_IN
    }

    /**
     * Check if booking is in the past.
     */
    fun isPast(): Boolean {
        return endTime.isBefore(LocalDateTime.now())
    }

    /**
     * Check if booking is upcoming.
     */
    fun isUpcoming(): Boolean {
        return startTime.isAfter(LocalDateTime.now()) && status == BookingStatus.CONFIRMED
    }

    /**
     * Check if booking can be cancelled.
     */
    fun canBeCancelled(cancellationHours: Int): Boolean {
        if (status != BookingStatus.CONFIRMED) {
            return false
        }

        val now = LocalDateTime.now()
        val hoursUntilStart = ChronoUnit.HOURS.between(now, startTime)
        return hoursUntilStart >= cancellationHours
    }

    /**
     * Cancel the booking.
     */
    fun cancel(reason: String, cancelledBy: String, refundAmount: BigDecimal? = null) {
        this.status = BookingStatus.CANCELLED
        this.statusReason = reason
        this.statusChangedAt = Instant.now()
        this.cancelledAt = Instant.now()
        this.cancelledBy = cancelledBy
        this.cancellationReason = reason
        this.refundAmount = refundAmount
    }

    /**
     * Complete the booking.
     */
    fun complete() {
        this.status = BookingStatus.COMPLETED
        this.statusChangedAt = Instant.now()
    }

    /**
     * Mark as no-show.
     */
    fun markAsNoShow() {
        this.status = BookingStatus.NO_SHOW
        this.statusChangedAt = Instant.now()
    }

    /**
     * Check-in for booking.
     */
    fun checkIn() {
        this.status = BookingStatus.CHECKED_IN
        this.checkedInAt = Instant.now()
        this.statusChangedAt = Instant.now()
    }

    /**
     * Check-out from booking.
     */
    fun checkOut() {
        this.status = BookingStatus.COMPLETED
        this.checkedOutAt = Instant.now()
        this.statusChangedAt = Instant.now()
    }

    /**
     * Get booking duration as human-readable string.
     */
    fun getDurationString(): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        return if (hours > 0 && minutes > 0) {
            "${hours}h ${minutes}m"
        } else if (hours > 0) {
            "${hours}h"
        } else {
            "${minutes}m"
        }
    }

    override fun toString(): String {
        return "Booking(number='$bookingNumber', member=${member.getFullName()}, court=${court.name}, " +
                "date=$bookingDate, time=$startTime-$endTime, status=$status)"
    }
}

/**
 * Booking status enum.
 */
enum class BookingStatus {
    PENDING,      // Awaiting confirmation or payment
    CONFIRMED,    // Booking confirmed
    CHECKED_IN,   // Player checked in
    COMPLETED,    // Booking completed
    CANCELLED,    // Cancelled by member or staff
    NO_SHOW,      // Member didn't show up
    RESCHEDULED   // Moved to different time
}

/**
 * Payment status enum for bookings.
 */
enum class PaymentStatus {
    PENDING,      // Payment not yet made
    PAID,         // Fully paid
    PARTIALLY_PAID, // Partial payment made
    REFUNDED,     // Payment refunded
    FAILED        // Payment failed
}
