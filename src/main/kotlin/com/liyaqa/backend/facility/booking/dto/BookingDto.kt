package com.liyaqa.backend.facility.booking.dto

import com.liyaqa.backend.facility.booking.domain.Booking
import com.liyaqa.backend.facility.booking.domain.BookingStatus
import com.liyaqa.backend.facility.booking.domain.PaymentStatus
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Request to create a new booking.
 */
data class BookingCreateRequest(
    @field:NotNull(message = "Member ID is required")
    val memberId: UUID,

    @field:NotNull(message = "Court ID is required")
    val courtId: UUID,

    @field:NotNull(message = "Booking date is required")
    @field:Future(message = "Booking date must be in the future")
    val bookingDate: LocalDate,

    @field:NotNull(message = "Start time is required")
    val startTime: LocalDateTime,

    @field:NotNull(message = "Duration is required")
    @field:Min(value = 30, message = "Minimum booking duration is 30 minutes")
    val durationMinutes: Int,

    val membershipId: UUID? = null, // Optional - to apply membership benefits
    val numberOfPlayers: Int = 2,
    val additionalPlayers: String? = null,
    val specialRequests: String? = null,
    val paymentMethod: String? = null
)

/**
 * Request to update booking.
 */
data class BookingUpdateRequest(
    val startTime: LocalDateTime? = null,
    val durationMinutes: Int? = null,
    val numberOfPlayers: Int? = null,
    val additionalPlayers: String? = null,
    val specialRequests: String? = null,
    val notes: String? = null
)

/**
 * Request to cancel booking.
 */
data class BookingCancelRequest(
    @field:NotBlank(message = "Cancellation reason is required")
    val reason: String
)

/**
 * Request to reschedule booking.
 */
data class BookingRescheduleRequest(
    @field:NotNull(message = "New start time is required")
    val newStartTime: LocalDateTime,

    @field:NotNull(message = "New court ID is required")
    val newCourtId: UUID,

    val reason: String? = null
)

/**
 * Request to check availability.
 */
data class AvailabilityCheckRequest(
    @field:NotNull(message = "Court ID is required")
    val courtId: UUID,

    @field:NotNull(message = "Start time is required")
    val startTime: LocalDateTime,

    @field:NotNull(message = "Duration is required")
    @field:Min(value = 30, message = "Minimum duration is 30 minutes")
    val durationMinutes: Int
)

/**
 * Response for availability check.
 */
data class AvailabilityResponse(
    val isAvailable: Boolean,
    val courtId: UUID,
    val courtName: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val conflictingBookings: List<BookingBasicResponse> = emptyList()
)

/**
 * Response DTO for booking with full details.
 */
data class BookingResponse(
    val id: UUID,
    val bookingNumber: String,

    // Member details
    val memberId: UUID,
    val memberName: String,
    val memberEmail: String,
    val memberPhone: String,

    // Court details
    val courtId: UUID,
    val courtName: String,
    val courtType: String,

    // Branch and Facility
    val branchId: UUID,
    val branchName: String,
    val facilityId: UUID,
    val facilityName: String,
    val tenantId: String,

    // Membership
    val membershipId: UUID?,
    val membershipNumber: String?,

    // Booking details
    val bookingDate: LocalDate,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationMinutes: Int,
    val durationString: String,

    // Status
    val status: BookingStatus,
    val statusReason: String?,
    val statusChangedAt: Instant?,

    // Pricing
    val originalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val finalPrice: BigDecimal,
    val currency: String,

    // Payment
    val paymentStatus: PaymentStatus,
    val paymentMethod: String?,
    val paymentReference: String?,
    val paidAt: Instant?,

    // Participants
    val numberOfPlayers: Int,
    val additionalPlayers: String?,
    val specialRequests: String?,

    // Cancellation
    val cancelledAt: Instant?,
    val cancelledBy: String?,
    val cancellationReason: String?,
    val refundAmount: BigDecimal?,

    // Check-in/out
    val checkedInAt: Instant?,
    val checkedOutAt: Instant?,

    // Notes
    val notes: String?,

    // Flags
    val reminderSent: Boolean,
    val confirmationSent: Boolean,

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(booking: Booking): BookingResponse {
            return BookingResponse(
                id = booking.id!!,
                bookingNumber = booking.bookingNumber,
                memberId = booking.member.id!!,
                memberName = booking.member.getFullName(),
                memberEmail = booking.member.email,
                memberPhone = booking.member.phoneNumber,
                courtId = booking.court.id!!,
                courtName = booking.court.name,
                courtType = booking.court.courtType.displayName,
                branchId = booking.branch.id!!,
                branchName = booking.branch.name,
                facilityId = booking.facility.id!!,
                facilityName = booking.facility.name,
                tenantId = booking.tenantId,
                membershipId = booking.membership?.id,
                membershipNumber = booking.membership?.membershipNumber,
                bookingDate = booking.bookingDate,
                startTime = booking.startTime,
                endTime = booking.endTime,
                durationMinutes = booking.durationMinutes,
                durationString = booking.getDurationString(),
                status = booking.status,
                statusReason = booking.statusReason,
                statusChangedAt = booking.statusChangedAt,
                originalPrice = booking.originalPrice,
                discountAmount = booking.discountAmount,
                finalPrice = booking.finalPrice,
                currency = booking.currency,
                paymentStatus = booking.paymentStatus,
                paymentMethod = booking.paymentMethod,
                paymentReference = booking.paymentReference,
                paidAt = booking.paidAt,
                numberOfPlayers = booking.numberOfPlayers,
                additionalPlayers = booking.additionalPlayers,
                specialRequests = booking.specialRequests,
                cancelledAt = booking.cancelledAt,
                cancelledBy = booking.cancelledBy,
                cancellationReason = booking.cancellationReason,
                refundAmount = booking.refundAmount,
                checkedInAt = booking.checkedInAt,
                checkedOutAt = booking.checkedOutAt,
                notes = booking.notes,
                reminderSent = booking.reminderSent,
                confirmationSent = booking.confirmationSent,
                createdAt = booking.createdAt,
                updatedAt = booking.updatedAt
            )
        }
    }
}

/**
 * Response DTO for booking with basic information (for lists).
 */
data class BookingBasicResponse(
    val id: UUID,
    val bookingNumber: String,
    val memberName: String,
    val courtName: String,
    val branchName: String,
    val bookingDate: LocalDate,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationString: String,
    val status: BookingStatus,
    val finalPrice: BigDecimal,
    val currency: String,
    val paymentStatus: PaymentStatus
) {
    companion object {
        fun from(booking: Booking): BookingBasicResponse {
            return BookingBasicResponse(
                id = booking.id!!,
                bookingNumber = booking.bookingNumber,
                memberName = booking.member.getFullName(),
                courtName = booking.court.name,
                branchName = booking.branch.name,
                bookingDate = booking.bookingDate,
                startTime = booking.startTime,
                endTime = booking.endTime,
                durationString = booking.getDurationString(),
                status = booking.status,
                finalPrice = booking.finalPrice,
                currency = booking.currency,
                paymentStatus = booking.paymentStatus
            )
        }
    }
}

/**
 * Response for time slot availability.
 */
data class TimeSlotResponse(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAvailable: Boolean,
    val price: BigDecimal
)

/**
 * Response for daily schedule.
 */
data class DailyScheduleResponse(
    val date: LocalDate,
    val courtId: UUID,
    val courtName: String,
    val bookings: List<BookingBasicResponse>,
    val availableSlots: List<TimeSlotResponse>
)
