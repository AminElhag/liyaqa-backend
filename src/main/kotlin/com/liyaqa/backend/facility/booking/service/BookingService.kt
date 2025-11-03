package com.liyaqa.backend.facility.booking.service

import com.liyaqa.backend.facility.booking.data.BookingRepository
import com.liyaqa.backend.facility.booking.data.CourtRepository
import com.liyaqa.backend.facility.booking.domain.Booking
import com.liyaqa.backend.facility.booking.domain.BookingStatus
import com.liyaqa.backend.facility.booking.domain.PaymentStatus
import com.liyaqa.backend.facility.booking.dto.*
import com.liyaqa.backend.facility.membership.data.MemberRepository
import com.liyaqa.backend.facility.membership.data.MembershipRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

/**
 * Service for managing bookings/reservations.
 */
@Service
@Transactional
class BookingService(
    private val bookingRepository: BookingRepository,
    private val courtRepository: CourtRepository,
    private val memberRepository: MemberRepository,
    private val membershipRepository: MembershipRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new booking.
     */
    fun createBooking(request: BookingCreateRequest): BookingResponse {
        // Validate member exists
        val member = memberRepository.findById(request.memberId)
            .orElseThrow { EntityNotFoundException("Member not found: ${request.memberId}") }

        // Validate court exists and is available
        val court = courtRepository.findById(request.courtId)
            .orElseThrow { EntityNotFoundException("Court not found: ${request.courtId}") }

        if (!court.isAvailableForBooking()) {
            throw IllegalStateException("Court is not available for booking")
        }

        // Validate booking duration
        if (request.durationMinutes < court.minBookingDuration) {
            throw IllegalArgumentException("Booking duration must be at least ${court.minBookingDuration} minutes")
        }

        if (request.durationMinutes > court.maxBookingDuration) {
            throw IllegalArgumentException("Booking duration cannot exceed ${court.maxBookingDuration} minutes")
        }

        // Calculate end time
        val endTime = request.startTime.plusMinutes(request.durationMinutes.toLong())

        // Check for overlapping bookings
        if (bookingRepository.hasOverlappingBooking(court.id!!, request.startTime, endTime)) {
            throw IllegalStateException("Court is already booked for the requested time")
        }

        // Validate membership if provided
        val membership = request.membershipId?.let {
            membershipRepository.findById(it)
                .orElseThrow { EntityNotFoundException("Membership not found: $it") }
        }

        // Validate membership belongs to member
        if (membership != null && membership.member.id != member.id) {
            throw IllegalArgumentException("Membership does not belong to the specified member")
        }

        // Calculate price
        val hourlyRate = court.hourlyRate
        val hours = request.durationMinutes.toBigDecimal().divide(BigDecimal(60))
        val originalPrice = hourlyRate.multiply(hours)

        // Apply membership discount if applicable
        var discountAmount = BigDecimal.ZERO
        // TODO: Implement membership benefit discount logic

        val finalPrice = originalPrice.subtract(discountAmount)

        // Generate booking number
        val bookingNumber = generateBookingNumber()

        val booking = Booking(
            member = member,
            court = court,
            branch = court.branch,
            facility = court.facility,
            membership = membership,
            bookingNumber = bookingNumber,
            bookingDate = request.bookingDate,
            startTime = request.startTime,
            endTime = endTime,
            durationMinutes = request.durationMinutes,
            originalPrice = originalPrice,
            discountAmount = discountAmount,
            finalPrice = finalPrice,
            currency = court.currency,
            numberOfPlayers = request.numberOfPlayers,
            additionalPlayers = request.additionalPlayers,
            specialRequests = request.specialRequests,
            paymentMethod = request.paymentMethod
        )

        booking.tenantId = court.tenantId

        // If membership provided, mark booking as using membership benefit
        if (membership != null) {
            membership.recordBooking()
            membershipRepository.save(membership)
        }

        val savedBooking = bookingRepository.save(booking)

        logger.info("Booking created: ${savedBooking.bookingNumber} for ${member.getFullName()} at ${court.name}")

        return BookingResponse.from(savedBooking)
    }

    /**
     * Get booking by ID.
     */
    @Transactional(readOnly = true)
    fun getBookingById(id: UUID): BookingResponse {
        val booking = bookingRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Booking not found: $id") }

        return BookingResponse.from(booking)
    }

    /**
     * Get booking by booking number.
     */
    @Transactional(readOnly = true)
    fun getBookingByNumber(bookingNumber: String): BookingResponse {
        val booking = bookingRepository.findByBookingNumber(bookingNumber)
            ?: throw EntityNotFoundException("Booking not found: $bookingNumber")

        return BookingResponse.from(booking)
    }

    /**
     * Update booking.
     */
    fun updateBooking(id: UUID, request: BookingUpdateRequest): BookingResponse {
        val booking = bookingRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Booking not found: $id") }

        if (booking.status != BookingStatus.CONFIRMED) {
            throw IllegalStateException("Only confirmed bookings can be updated")
        }

        request.startTime?.let {
            // Recalculate end time if start time changes
            val newEndTime = it.plusMinutes(booking.durationMinutes.toLong())

            // Check for overlapping bookings (excluding current booking)
            val courtId = booking.court.id!!
            val bookingId = booking.id!!
            val overlappingBookings = bookingRepository.findByCourtIdAndDateRange(
                courtId,
                it,
                newEndTime
            ).filter { b -> b.id != bookingId }

            if (overlappingBookings.isNotEmpty()) {
                throw IllegalStateException("Court is already booked for the requested time")
            }

            booking.startTime = it
            booking.endTime = newEndTime
            booking.bookingDate = it.toLocalDate()
        }

        request.durationMinutes?.let {
            if (it < booking.court.minBookingDuration || it > booking.court.maxBookingDuration) {
                throw IllegalArgumentException("Invalid booking duration")
            }

            booking.durationMinutes = it
            booking.endTime = booking.startTime.plusMinutes(it.toLong())

            // Recalculate price
            val hours = it.toBigDecimal().divide(BigDecimal(60))
            booking.originalPrice = booking.court.hourlyRate.multiply(hours)
            booking.finalPrice = booking.originalPrice.subtract(booking.discountAmount)
        }

        request.numberOfPlayers?.let { booking.numberOfPlayers = it }
        request.additionalPlayers?.let { booking.additionalPlayers = it }
        request.specialRequests?.let { booking.specialRequests = it }
        request.notes?.let { booking.notes = it }

        val savedBooking = bookingRepository.save(booking)

        logger.info("Booking updated: ${savedBooking.bookingNumber}")

        return BookingResponse.from(savedBooking)
    }

    /**
     * Cancel booking.
     */
    fun cancelBooking(id: UUID, request: BookingCancelRequest): BookingResponse {
        val booking = bookingRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Booking not found: $id") }

        if (booking.status != BookingStatus.CONFIRMED) {
            throw IllegalStateException("Only confirmed bookings can be cancelled")
        }

        // Check if booking can be cancelled based on cancellation policy
        if (!booking.canBeCancelled(booking.court.cancellationHours)) {
            throw IllegalStateException(
                "Booking cannot be cancelled within ${booking.court.cancellationHours} hours of start time"
            )
        }

        // Calculate refund (simple logic - full refund if cancelled in time)
        val refundAmount = if (booking.paymentStatus == PaymentStatus.PAID) {
            booking.finalPrice
        } else {
            BigDecimal.ZERO
        }

        booking.cancel(request.reason, booking.member.getFullName(), refundAmount)

        // Update payment status if refund applicable
        if (refundAmount > BigDecimal.ZERO) {
            booking.paymentStatus = PaymentStatus.REFUNDED
        }

        val savedBooking = bookingRepository.save(booking)

        logger.info("Booking cancelled: ${savedBooking.bookingNumber} - Reason: ${request.reason}")

        return BookingResponse.from(savedBooking)
    }

    /**
     * Check-in for booking.
     */
    fun checkInBooking(id: UUID): BookingResponse {
        val booking = bookingRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Booking not found: $id") }

        if (booking.status != BookingStatus.CONFIRMED) {
            throw IllegalStateException("Only confirmed bookings can be checked in")
        }

        booking.checkIn()
        val savedBooking = bookingRepository.save(booking)

        logger.info("Member checked in: ${savedBooking.bookingNumber}")

        return BookingResponse.from(savedBooking)
    }

    /**
     * Check-out from booking.
     */
    fun checkOutBooking(id: UUID): BookingResponse {
        val booking = bookingRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Booking not found: $id") }

        if (booking.status != BookingStatus.CHECKED_IN) {
            throw IllegalStateException("Only checked-in bookings can be checked out")
        }

        booking.checkOut()
        val savedBooking = bookingRepository.save(booking)

        logger.info("Member checked out: ${savedBooking.bookingNumber}")

        return BookingResponse.from(savedBooking)
    }

    /**
     * Mark booking as no-show.
     */
    fun markAsNoShow(id: UUID): BookingResponse {
        val booking = bookingRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Booking not found: $id") }

        if (booking.status != BookingStatus.CONFIRMED) {
            throw IllegalStateException("Only confirmed bookings can be marked as no-show")
        }

        booking.markAsNoShow()
        val savedBooking = bookingRepository.save(booking)

        logger.warn("Booking marked as no-show: ${savedBooking.bookingNumber}")

        return BookingResponse.from(savedBooking)
    }

    /**
     * Check availability for a time slot.
     */
    @Transactional(readOnly = true)
    fun checkAvailability(request: AvailabilityCheckRequest): AvailabilityResponse {
        val court = courtRepository.findById(request.courtId)
            .orElseThrow { EntityNotFoundException("Court not found: ${request.courtId}") }

        val courtId = court.id!!
        val endTime = request.startTime.plusMinutes(request.durationMinutes.toLong())

        val isAvailable = !bookingRepository.hasOverlappingBooking(
            courtId,
            request.startTime,
            endTime
        )

        val conflictingBookings = if (!isAvailable) {
            bookingRepository.findByCourtIdAndDateRange(courtId, request.startTime, endTime)
                .map { BookingBasicResponse.from(it) }
        } else {
            emptyList()
        }

        return AvailabilityResponse(
            isAvailable = isAvailable,
            courtId = courtId,
            courtName = court.name,
            startTime = request.startTime,
            endTime = endTime,
            conflictingBookings = conflictingBookings
        )
    }

    /**
     * Get available time slots for a court on a specific date.
     */
    @Transactional(readOnly = true)
    fun getAvailableSlots(courtId: UUID, date: LocalDate): List<TimeSlotResponse> {
        val court = courtRepository.findById(courtId)
            .orElseThrow { EntityNotFoundException("Court not found: $courtId") }

        // Get existing bookings for the date
        val bookings = bookingRepository.findByCourtIdAndDate(courtId, date)

        // Generate all possible time slots (e.g., 8 AM to 10 PM)
        val slots = mutableListOf<TimeSlotResponse>()
        var currentTime = LocalDateTime.of(date, LocalTime.of(8, 0))
        val endOfDay = LocalDateTime.of(date, LocalTime.of(22, 0))

        while (currentTime.isBefore(endOfDay)) {
            val slotEnd = currentTime.plusMinutes(court.bookingInterval.toLong())

            // Check if slot conflicts with any booking
            val isAvailable = bookings.none { booking ->
                booking.startTime.isBefore(slotEnd) && booking.endTime.isAfter(currentTime)
            }

            slots.add(
                TimeSlotResponse(
                    startTime = currentTime,
                    endTime = slotEnd,
                    isAvailable = isAvailable,
                    price = court.hourlyRate
                )
            )

            currentTime = slotEnd
        }

        return slots
    }

    /**
     * Get bookings by member.
     */
    @Transactional(readOnly = true)
    fun getBookingsByMember(memberId: UUID, pageable: Pageable): Page<BookingBasicResponse> {
        return bookingRepository.findByMemberId(memberId, pageable)
            .map { BookingBasicResponse.from(it) }
    }

    /**
     * Get upcoming bookings for member.
     */
    @Transactional(readOnly = true)
    fun getUpcomingBookingsByMember(memberId: UUID): List<BookingBasicResponse> {
        return bookingRepository.findUpcomingByMemberId(memberId, LocalDateTime.now())
            .map { BookingBasicResponse.from(it) }
    }

    /**
     * Search bookings with filters.
     */
    @Transactional(readOnly = true)
    fun searchBookings(
        branchId: UUID?,
        courtId: UUID?,
        memberId: UUID?,
        status: BookingStatus?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageable: Pageable
    ): Page<BookingBasicResponse> {
        return bookingRepository.searchBookings(
            branchId, courtId, memberId, status, startDate, endDate, pageable
        ).map { BookingBasicResponse.from(it) }
    }

    /**
     * Get bookings by branch and date.
     */
    @Transactional(readOnly = true)
    fun getBookingsByBranchAndDate(branchId: UUID, date: LocalDate): List<BookingBasicResponse> {
        return bookingRepository.findByBranchIdAndDate(branchId, date)
            .map { BookingBasicResponse.from(it) }
    }

    /**
     * Get today's bookings by branch.
     */
    @Transactional(readOnly = true)
    fun getTodaysBookingsByBranch(branchId: UUID): List<BookingBasicResponse> {
        return bookingRepository.findTodaysByBranchId(branchId, LocalDate.now())
            .map { BookingBasicResponse.from(it) }
    }

    /**
     * Generate unique booking number.
     */
    private fun generateBookingNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "BKG-${timestamp}-${random}"
    }
}
