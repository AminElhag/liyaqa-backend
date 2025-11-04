package com.liyaqa.backend.facility.trainer.controller

import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.facility.trainer.data.TrainerRepository
import com.liyaqa.backend.facility.trainer.domain.Trainer
import com.liyaqa.backend.facility.trainer.domain.TrainerBooking
import com.liyaqa.backend.facility.trainer.domain.TrainerStatus
import com.liyaqa.backend.facility.trainer.service.TrainerBookingService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

/**
 * Member-facing controller for trainer bookings.
 *
 * Endpoints for members to:
 * - Browse available trainers
 * - Book trainer sessions
 * - View their bookings
 * - Cancel bookings
 */
@RestController
@RequestMapping("/api/v1/member/trainers")
class MemberTrainerController(
    private val trainerRepository: TrainerRepository,
    private val trainerBookingService: TrainerBookingService
) {

    /**
     * List available trainers.
     * GET /api/v1/member/trainers
     */
    @GetMapping
    fun listTrainers(
        @AuthenticationPrincipal member: Member,
        @RequestParam(required = false) facilityId: UUID?
    ): ResponseEntity<List<Trainer>> {
        val trainers = if (facilityId != null) {
            trainerRepository.findAvailableTrainersByFacility(facilityId)
        } else {
            trainerRepository.findByTenantIdAndStatusOrderByFirstNameAsc(member.tenantId, TrainerStatus.ACTIVE)
        }

        return ResponseEntity.ok(trainers)
    }

    /**
     * Get trainer details.
     * GET /api/v1/member/trainers/{trainerId}
     */
    @GetMapping("/{trainerId}")
    fun getTrainer(
        @AuthenticationPrincipal member: Member,
        @PathVariable trainerId: UUID
    ): ResponseEntity<Trainer> {
        val trainer = trainerRepository.findById(trainerId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // Verify trainer belongs to member's tenant
        if (trainer.tenantId != member.tenantId) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(trainer)
    }

    /**
     * Book a trainer session.
     * POST /api/v1/member/trainers/{trainerId}/book
     */
    @PostMapping("/{trainerId}/book")
    fun bookTrainer(
        @AuthenticationPrincipal member: Member,
        @PathVariable trainerId: UUID,
        @RequestBody request: BookTrainerRequest
    ): ResponseEntity<TrainerBooking> {
        try {
            val booking = trainerBookingService.createBooking(
                tenantId = member.tenantId,
                trainerId = trainerId,
                memberId = member.id!!,
                startTime = request.startTime,
                endTime = request.endTime,
                sessionType = request.sessionType,
                sessionFocus = request.sessionFocus,
                location = request.location,
                specialRequests = request.specialRequests
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(booking)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Get member's trainer bookings.
     * GET /api/v1/member/trainers/bookings
     */
    @GetMapping("/bookings")
    fun getMyBookings(
        @AuthenticationPrincipal member: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<TrainerBooking>> {
        val pageable = PageRequest.of(page, size)
        val bookings = trainerBookingService.getMemberBookings(member.id!!, pageable)

        return ResponseEntity.ok(bookings)
    }

    /**
     * Cancel a booking.
     * POST /api/v1/member/trainers/bookings/{bookingId}/cancel
     */
    @PostMapping("/bookings/{bookingId}/cancel")
    fun cancelBooking(
        @AuthenticationPrincipal member: Member,
        @PathVariable bookingId: UUID,
        @RequestBody request: CancelBookingRequest
    ): ResponseEntity<TrainerBooking> {
        try {
            val booking = trainerBookingService.getBooking(bookingId)
                ?: return ResponseEntity.notFound().build()

            // Verify booking belongs to member
            if (booking.member.id != member.id) {
                return ResponseEntity.notFound().build()
            }

            val cancelled = trainerBookingService.cancelBooking(
                bookingId = bookingId,
                reason = request.reason ?: "Cancelled by member",
                cancelledBy = member.getFullName()
            )

            return ResponseEntity.ok(cancelled)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
    }
}

/**
 * Request DTOs
 */
data class BookTrainerRequest(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val sessionType: com.liyaqa.backend.facility.trainer.domain.SessionType,
    val sessionFocus: String? = null,
    val location: String? = null,
    val specialRequests: String? = null
)

data class CancelBookingRequest(
    val reason: String?
)
