package com.liyaqa.backend.facility.trainer.service

import com.liyaqa.backend.facility.membership.data.MemberRepository
import com.liyaqa.backend.facility.trainer.data.TrainerBookingRepository
import com.liyaqa.backend.facility.trainer.data.TrainerRepository
import com.liyaqa.backend.facility.trainer.domain.TrainerBooking
import com.liyaqa.backend.facility.trainer.domain.TrainerBookingStatus
import com.liyaqa.backend.internal.facility.data.FacilityBranchRepository
import com.liyaqa.backend.internal.facility.data.SportFacilityRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Service for managing trainer bookings.
 *
 * Design Philosophy:
 * "Every booking is a promise to help someone become their better self."
 */
@Service
@Transactional
class TrainerBookingService(
    private val trainerBookingRepository: TrainerBookingRepository,
    private val trainerRepository: TrainerRepository,
    private val memberRepository: MemberRepository,
    private val facilityRepository: SportFacilityRepository,
    private val branchRepository: FacilityBranchRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new trainer booking.
     */
    fun createBooking(
        tenantId: String,
        trainerId: UUID,
        memberId: UUID,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        sessionType: com.liyaqa.backend.facility.trainer.domain.SessionType,
        sessionFocus: String? = null,
        location: String? = null,
        specialRequests: String? = null
    ): TrainerBooking {
        val trainer = trainerRepository.findById(trainerId).orElseThrow {
            IllegalArgumentException("Trainer not found")
        }

        val member = memberRepository.findById(memberId).orElseThrow {
            IllegalArgumentException("Member not found")
        }

        // Check for conflicts
        val durationMinutes = ((endTime.toLocalTime().toSecondOfDay() - startTime.toLocalTime().toSecondOfDay()) / 60).toInt()
        val conflicts = trainerBookingRepository.countConflictingBookings(trainerId, startTime, endTime)

        if (conflicts > 0) {
            throw IllegalStateException("Trainer is not available at this time")
        }

        // Calculate price
        val price = trainer.getRateForDuration(durationMinutes) ?: throw IllegalStateException("Unable to calculate price")

        // Create booking
        val booking = TrainerBooking(
            trainer = trainer,
            member = member,
            facility = trainer.facility,
            branch = trainer.branch,
            bookingNumber = generateBookingNumber(),
            sessionDate = startTime.toLocalDate(),
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes,
            sessionType = sessionType,
            sessionFocus = sessionFocus,
            price = price,
            finalPrice = price,
            currency = trainer.currency,
            location = location,
            specialRequests = specialRequests
        )

        booking.tenantId = tenantId

        val saved = trainerBookingRepository.save(booking)

        logger.info("Created trainer booking: id={}, trainer={}, member={}, date={}",
            saved.id, trainerId, memberId, startTime)

        return saved
    }

    /**
     * Get booking by ID.
     */
    fun getBooking(bookingId: UUID): TrainerBooking? {
        return trainerBookingRepository.findById(bookingId).orElse(null)
    }

    /**
     * Get member's bookings.
     */
    fun getMemberBookings(memberId: UUID, pageable: Pageable): Page<TrainerBooking> {
        return trainerBookingRepository.findByMember_IdOrderBySessionDateDescStartTimeDesc(memberId, pageable)
    }

    /**
     * Get trainer's bookings.
     */
    fun getTrainerBookings(trainerId: UUID, pageable: Pageable): Page<TrainerBooking> {
        return trainerBookingRepository.findByTrainer_IdOrderBySessionDateDescStartTimeDesc(trainerId, pageable)
    }

    /**
     * Get trainer's bookings for a specific date.
     */
    fun getTrainerBookingsForDate(trainerId: UUID, date: LocalDate): List<TrainerBooking> {
        return trainerBookingRepository.findByTrainerAndDate(trainerId, date)
    }

    /**
     * Cancel a booking.
     */
    fun cancelBooking(bookingId: UUID, reason: String, cancelledBy: String): TrainerBooking {
        val booking = trainerBookingRepository.findById(bookingId).orElseThrow {
            IllegalArgumentException("Booking not found")
        }

        if (!booking.isCancellable()) {
            throw IllegalStateException("Booking cannot be cancelled in its current state")
        }

        booking.cancel(reason, cancelledBy)

        val saved = trainerBookingRepository.save(booking)

        logger.info("Cancelled trainer booking: id={}, reason={}", bookingId, reason)

        return saved
    }

    /**
     * Complete a booking.
     */
    fun completeBooking(
        bookingId: UUID,
        trainerNotes: String? = null,
        memberPerformanceRating: Int? = null
    ): TrainerBooking {
        val booking = trainerBookingRepository.findById(bookingId).orElseThrow {
            IllegalArgumentException("Booking not found")
        }

        booking.complete()
        booking.trainerNotes = trainerNotes
        booking.memberPerformanceRating = memberPerformanceRating

        // Update trainer's session count
        booking.trainer.incrementSessionCount()
        trainerRepository.save(booking.trainer)

        val saved = trainerBookingRepository.save(booking)

        logger.info("Completed trainer booking: id={}", bookingId)

        return saved
    }

    /**
     * Generate unique booking number.
     */
    private fun generateBookingNumber(): String {
        return "TB${System.currentTimeMillis()}"
    }
}
