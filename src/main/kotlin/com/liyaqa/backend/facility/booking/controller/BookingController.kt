package com.liyaqa.backend.facility.booking.controller

import com.liyaqa.backend.facility.booking.domain.BookingStatus
import com.liyaqa.backend.facility.booking.dto.*
import com.liyaqa.backend.facility.booking.service.BookingService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

/**
 * REST API for managing bookings/reservations.
 *
 * Base path: /api/v1/facility/bookings
 */
@RestController
@RequestMapping("/api/v1/facility/bookings")
class BookingController(
    private val bookingService: BookingService
) {

    /**
     * Create a new booking.
     * POST /api/v1/facility/bookings
     */
    @PostMapping
    fun createBooking(
        @Valid @RequestBody request: BookingCreateRequest
    ): ResponseEntity<BookingResponse> {
        val booking = bookingService.createBooking(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(booking)
    }

    /**
     * Get booking by ID.
     * GET /api/v1/facility/bookings/{id}
     */
    @GetMapping("/{id}")
    fun getBookingById(
        @PathVariable id: UUID
    ): ResponseEntity<BookingResponse> {
        val booking = bookingService.getBookingById(id)
        return ResponseEntity.ok(booking)
    }

    /**
     * Get booking by booking number.
     * GET /api/v1/facility/bookings/by-number/{bookingNumber}
     */
    @GetMapping("/by-number/{bookingNumber}")
    fun getBookingByNumber(
        @PathVariable bookingNumber: String
    ): ResponseEntity<BookingResponse> {
        val booking = bookingService.getBookingByNumber(bookingNumber)
        return ResponseEntity.ok(booking)
    }

    /**
     * Update booking.
     * PUT /api/v1/facility/bookings/{id}
     */
    @PutMapping("/{id}")
    fun updateBooking(
        @PathVariable id: UUID,
        @Valid @RequestBody request: BookingUpdateRequest
    ): ResponseEntity<BookingResponse> {
        val booking = bookingService.updateBooking(id, request)
        return ResponseEntity.ok(booking)
    }

    /**
     * Cancel booking.
     * POST /api/v1/facility/bookings/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    fun cancelBooking(
        @PathVariable id: UUID,
        @Valid @RequestBody request: BookingCancelRequest
    ): ResponseEntity<BookingResponse> {
        val booking = bookingService.cancelBooking(id, request)
        return ResponseEntity.ok(booking)
    }

    /**
     * Check-in for booking.
     * POST /api/v1/facility/bookings/{id}/check-in
     */
    @PostMapping("/{id}/check-in")
    fun checkInBooking(
        @PathVariable id: UUID
    ): ResponseEntity<BookingResponse> {
        val booking = bookingService.checkInBooking(id)
        return ResponseEntity.ok(booking)
    }

    /**
     * Check-out from booking.
     * POST /api/v1/facility/bookings/{id}/check-out
     */
    @PostMapping("/{id}/check-out")
    fun checkOutBooking(
        @PathVariable id: UUID
    ): ResponseEntity<BookingResponse> {
        val booking = bookingService.checkOutBooking(id)
        return ResponseEntity.ok(booking)
    }

    /**
     * Mark booking as no-show.
     * POST /api/v1/facility/bookings/{id}/no-show
     */
    @PostMapping("/{id}/no-show")
    fun markAsNoShow(
        @PathVariable id: UUID
    ): ResponseEntity<BookingResponse> {
        val booking = bookingService.markAsNoShow(id)
        return ResponseEntity.ok(booking)
    }

    /**
     * Check availability for a time slot.
     * POST /api/v1/facility/bookings/check-availability
     */
    @PostMapping("/check-availability")
    fun checkAvailability(
        @Valid @RequestBody request: AvailabilityCheckRequest
    ): ResponseEntity<AvailabilityResponse> {
        val availability = bookingService.checkAvailability(request)
        return ResponseEntity.ok(availability)
    }

    /**
     * Get available time slots for a court on a specific date.
     * GET /api/v1/facility/bookings/available-slots
     */
    @GetMapping("/available-slots")
    fun getAvailableSlots(
        @RequestParam courtId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<TimeSlotResponse>> {
        val slots = bookingService.getAvailableSlots(courtId, date)
        return ResponseEntity.ok(slots)
    }

    /**
     * Get bookings by member.
     * GET /api/v1/facility/bookings/by-member/{memberId}
     */
    @GetMapping("/by-member/{memberId}")
    fun getBookingsByMember(
        @PathVariable memberId: UUID,
        @PageableDefault(size = 20, sort = ["startTime"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<BookingBasicResponse>> {
        val bookings = bookingService.getBookingsByMember(memberId, pageable)
        return ResponseEntity.ok(bookings)
    }

    /**
     * Get upcoming bookings for member.
     * GET /api/v1/facility/bookings/by-member/{memberId}/upcoming
     */
    @GetMapping("/by-member/{memberId}/upcoming")
    fun getUpcomingBookingsByMember(
        @PathVariable memberId: UUID
    ): ResponseEntity<List<BookingBasicResponse>> {
        val bookings = bookingService.getUpcomingBookingsByMember(memberId)
        return ResponseEntity.ok(bookings)
    }

    /**
     * Search bookings with filters.
     * GET /api/v1/facility/bookings/search
     */
    @GetMapping("/search")
    fun searchBookings(
        @RequestParam(required = false) branchId: UUID?,
        @RequestParam(required = false) courtId: UUID?,
        @RequestParam(required = false) memberId: UUID?,
        @RequestParam(required = false) status: BookingStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @PageableDefault(size = 20, sort = ["startTime"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<BookingBasicResponse>> {
        val bookings = bookingService.searchBookings(
            branchId, courtId, memberId, status, startDate, endDate, pageable
        )
        return ResponseEntity.ok(bookings)
    }

    /**
     * Get bookings by branch and date.
     * GET /api/v1/facility/bookings/by-branch/{branchId}/by-date
     */
    @GetMapping("/by-branch/{branchId}/by-date")
    fun getBookingsByBranchAndDate(
        @PathVariable branchId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<BookingBasicResponse>> {
        val bookings = bookingService.getBookingsByBranchAndDate(branchId, date)
        return ResponseEntity.ok(bookings)
    }

    /**
     * Get today's bookings by branch.
     * GET /api/v1/facility/bookings/by-branch/{branchId}/today
     */
    @GetMapping("/by-branch/{branchId}/today")
    fun getTodaysBookingsByBranch(
        @PathVariable branchId: UUID
    ): ResponseEntity<List<BookingBasicResponse>> {
        val bookings = bookingService.getTodaysBookingsByBranch(branchId)
        return ResponseEntity.ok(bookings)
    }
}
