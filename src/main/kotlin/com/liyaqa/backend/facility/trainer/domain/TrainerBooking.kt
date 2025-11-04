package com.liyaqa.backend.facility.trainer.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.facility.domain.SportFacility
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 * Trainer booking/session entity.
 *
 * Design Philosophy:
 * "Every session is an opportunity to transform someone's life."
 *
 * Represents a booked training session between a member and a trainer.
 */
@Entity
@Table(
    name = "trainer_bookings",
    indexes = [
        Index(name = "idx_trainer_booking_trainer", columnList = "trainer_id"),
        Index(name = "idx_trainer_booking_member", columnList = "member_id"),
        Index(name = "idx_trainer_booking_facility", columnList = "facility_id"),
        Index(name = "idx_trainer_booking_branch", columnList = "branch_id"),
        Index(name = "idx_trainer_booking_status", columnList = "status"),
        Index(name = "idx_trainer_booking_date", columnList = "session_date"),
        Index(name = "idx_trainer_booking_number", columnList = "booking_number"),
        Index(name = "idx_trainer_booking_tenant", columnList = "tenant_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_trainer_booking_number", columnNames = ["booking_number"])
    ]
)
class TrainerBooking(
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    var trainer: Trainer,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    var branch: FacilityBranch,

    // Booking Details
    @Column(name = "booking_number", length = 50, nullable = false, unique = true)
    var bookingNumber: String,

    @Column(name = "session_date", nullable = false)
    var sessionDate: java.time.LocalDate,

    @Column(name = "start_time", nullable = false)
    var startTime: LocalDateTime,

    @Column(name = "end_time", nullable = false)
    var endTime: LocalDateTime,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int,

    // Session Type
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 50)
    var sessionType: SessionType = SessionType.PERSONAL,

    @Column(name = "session_focus", length = 255)
    var sessionFocus: String? = null, // E.g., "Weight Loss", "Strength Training"

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: TrainerBookingStatus = TrainerBookingStatus.PENDING,

    @Column(name = "status_reason", columnDefinition = "TEXT")
    var statusReason: String? = null,

    @Column(name = "status_changed_at")
    var statusChangedAt: Instant? = null,

    // Pricing
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    var price: BigDecimal,

    @Column(name = "discount_amount", precision = 10, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "final_price", precision = 10, scale = 2, nullable = false)
    var finalPrice: BigDecimal,

    @Column(name = "currency", length = 3, nullable = false)
    var currency: String = "USD",

    // Payment
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "payment_method", length = 50)
    var paymentMethod: String? = null,

    @Column(name = "payment_reference", length = 255)
    var paymentReference: String? = null,

    @Column(name = "paid_at")
    var paidAt: Instant? = null,

    // Session Details
    @Column(name = "location", length = 255)
    var location: String? = null, // E.g., "Gym Floor", "Studio A", "Outdoor Track"

    @Column(name = "special_requests", columnDefinition = "TEXT")
    var specialRequests: String? = null,

    @Column(name = "member_goals", columnDefinition = "TEXT")
    var memberGoals: String? = null,

    @Column(name = "health_notes", columnDefinition = "TEXT")
    var healthNotes: String? = null, // Medical conditions, injuries, etc.

    // Check-in/Check-out
    @Column(name = "checked_in_at")
    var checkedInAt: Instant? = null,

    @Column(name = "checked_out_at")
    var checkedOutAt: Instant? = null,

    // Session Notes (filled by trainer after session)
    @Column(name = "trainer_notes", columnDefinition = "TEXT")
    var trainerNotes: String? = null,

    @Column(name = "exercises_performed", columnDefinition = "TEXT")
    var exercisesPerformed: String? = null, // JSON array

    @Column(name = "member_performance_rating")
    var memberPerformanceRating: Int? = null, // 1-5

    // Cancellation
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,

    @Column(name = "cancelled_by", length = 255)
    var cancelledBy: String? = null,

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    var cancellationReason: String? = null,

    @Column(name = "refund_amount", precision = 10, scale = 2)
    var refundAmount: BigDecimal? = null,

    // Rescheduling
    @Column(name = "rescheduled_from_id")
    var rescheduledFromId: UUID? = null,

    @Column(name = "rescheduled_to_id")
    var rescheduledToId: UUID? = null,

    // Notifications
    @Column(name = "reminder_sent")
    var reminderSent: Boolean = false,

    @Column(name = "reminder_sent_at")
    var reminderSentAt: Instant? = null

) : BaseEntity() {

    /**
     * Mark as checked in.
     */
    fun checkIn() {
        this.checkedInAt = Instant.now()
        this.status = TrainerBookingStatus.IN_PROGRESS
    }

    /**
     * Mark as completed.
     */
    fun complete() {
        this.checkedOutAt = Instant.now()
        this.status = TrainerBookingStatus.COMPLETED
        this.statusChangedAt = Instant.now()
    }

    /**
     * Cancel booking.
     */
    fun cancel(reason: String, cancelledBy: String) {
        this.status = TrainerBookingStatus.CANCELLED
        this.cancelledAt = Instant.now()
        this.cancellationReason = reason
        this.cancelledBy = cancelledBy
        this.statusChangedAt = Instant.now()
    }

    /**
     * Check if booking can be cancelled.
     */
    fun isCancellable(): Boolean {
        return status in listOf(
            TrainerBookingStatus.PENDING,
            TrainerBookingStatus.CONFIRMED
        )
    }

    /**
     * Check if booking can be rescheduled.
     */
    fun isReschedulable(): Boolean {
        return status in listOf(
            TrainerBookingStatus.PENDING,
            TrainerBookingStatus.CONFIRMED
        ) && startTime.isAfter(LocalDateTime.now())
    }
}

/**
 * Trainer booking status.
 */
enum class TrainerBookingStatus {
    PENDING,       // Awaiting confirmation
    CONFIRMED,     // Confirmed by trainer
    IN_PROGRESS,   // Session is currently happening
    COMPLETED,     // Session completed
    CANCELLED,     // Cancelled by member or trainer
    NO_SHOW,       // Member didn't show up
    RESCHEDULED    // Rescheduled to another time
}

/**
 * Session type.
 */
enum class SessionType {
    PERSONAL,      // One-on-one personal training
    SEMI_PRIVATE,  // 2-3 people
    GROUP,         // Group class
    ASSESSMENT,    // Fitness assessment
    CONSULTATION   // Initial consultation
}

/**
 * Payment status enum (same as court bookings for consistency).
 */
enum class PaymentStatus {
    PENDING,
    PAID,
    PARTIALLY_PAID,
    REFUNDED,
    FAILED
}
