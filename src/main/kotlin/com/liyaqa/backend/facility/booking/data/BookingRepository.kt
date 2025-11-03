package com.liyaqa.backend.facility.booking.data

import com.liyaqa.backend.facility.booking.domain.Booking
import com.liyaqa.backend.facility.booking.domain.BookingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Repository for Booking entity with comprehensive querying capabilities.
 */
@Repository
interface BookingRepository : JpaRepository<Booking, UUID> {

    /**
     * Find bookings by member.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.member.id = :memberId
        ORDER BY b.startTime DESC
    """)
    fun findByMemberId(@Param("memberId") memberId: UUID): List<Booking>

    /**
     * Find bookings by member with pagination.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.member.id = :memberId
        ORDER BY b.startTime DESC
    """)
    fun findByMemberId(
        @Param("memberId") memberId: UUID,
        pageable: Pageable
    ): Page<Booking>

    /**
     * Find upcoming bookings for member.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.member.id = :memberId
        AND b.startTime > :now
        AND b.status = 'CONFIRMED'
        ORDER BY b.startTime ASC
    """)
    fun findUpcomingByMemberId(
        @Param("memberId") memberId: UUID,
        @Param("now") now: LocalDateTime
    ): List<Booking>

    /**
     * Find bookings by court and date range.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.court.id = :courtId
        AND b.startTime >= :startTime
        AND b.endTime <= :endTime
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        ORDER BY b.startTime ASC
    """)
    fun findByCourtIdAndDateRange(
        @Param("courtId") courtId: UUID,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): List<Booking>

    /**
     * Find bookings by court and date.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.court.id = :courtId
        AND b.bookingDate = :date
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        ORDER BY b.startTime ASC
    """)
    fun findByCourtIdAndDate(
        @Param("courtId") courtId: UUID,
        @Param("date") date: LocalDate
    ): List<Booking>

    /**
     * Check for overlapping bookings.
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.court.id = :courtId
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        AND (
            (b.startTime < :endTime AND b.endTime > :startTime)
        )
    """)
    fun hasOverlappingBooking(
        @Param("courtId") courtId: UUID,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): Boolean

    /**
     * Find bookings by branch and date range.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.branch.id = :branchId
        AND b.startTime >= :startTime
        AND b.endTime <= :endTime
        ORDER BY b.startTime ASC
    """)
    fun findByBranchIdAndDateRange(
        @Param("branchId") branchId: UUID,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): List<Booking>

    /**
     * Find bookings by branch and date.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.branch.id = :branchId
        AND b.bookingDate = :date
        ORDER BY b.startTime ASC
    """)
    fun findByBranchIdAndDate(
        @Param("branchId") branchId: UUID,
        @Param("date") date: LocalDate
    ): List<Booking>

    /**
     * Find bookings by status.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.branch.id = :branchId
        AND b.status = :status
        ORDER BY b.startTime DESC
    """)
    fun findByBranchIdAndStatus(
        @Param("branchId") branchId: UUID,
        @Param("status") status: BookingStatus
    ): List<Booking>

    /**
     * Find booking by booking number.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.bookingNumber = :bookingNumber
    """)
    fun findByBookingNumber(@Param("bookingNumber") bookingNumber: String): Booking?

    /**
     * Search bookings with filters.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE (:branchId IS NULL OR b.branch.id = :branchId)
        AND (:courtId IS NULL OR b.court.id = :courtId)
        AND (:memberId IS NULL OR b.member.id = :memberId)
        AND (:status IS NULL OR b.status = :status)
        AND (:startDate IS NULL OR b.bookingDate >= :startDate)
        AND (:endDate IS NULL OR b.bookingDate <= :endDate)
        ORDER BY b.startTime DESC
    """)
    fun searchBookings(
        @Param("branchId") branchId: UUID?,
        @Param("courtId") courtId: UUID?,
        @Param("memberId") memberId: UUID?,
        @Param("status") status: BookingStatus?,
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?,
        pageable: Pageable
    ): Page<Booking>

    /**
     * Count bookings by member.
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.member.id = :memberId
        AND b.status = :status
    """)
    fun countByMemberIdAndStatus(
        @Param("memberId") memberId: UUID,
        @Param("status") status: BookingStatus
    ): Long

    /**
     * Count bookings for member in date range.
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.member.id = :memberId
        AND b.bookingDate >= :startDate
        AND b.bookingDate <= :endDate
        AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'COMPLETED')
    """)
    fun countByMemberIdAndDateRange(
        @Param("memberId") memberId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): Long

    /**
     * Find bookings expiring soon (for reminders).
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.startTime BETWEEN :startTime AND :endTime
        AND b.status = 'CONFIRMED'
        AND b.reminderSent = false
        ORDER BY b.startTime ASC
    """)
    fun findBookingsForReminder(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): List<Booking>

    /**
     * Find today's bookings by branch.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.branch.id = :branchId
        AND b.bookingDate = :today
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        ORDER BY b.startTime ASC
    """)
    fun findTodaysByBranchId(
        @Param("branchId") branchId: UUID,
        @Param("today") today: LocalDate
    ): List<Booking>

    /**
     * Count bookings by branch and date.
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.branch.id = :branchId
        AND b.bookingDate = :date
        AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'COMPLETED')
    """)
    fun countByBranchIdAndDate(
        @Param("branchId") branchId: UUID,
        @Param("date") date: LocalDate
    ): Long

    /**
     * Get revenue by branch and date range.
     */
    @Query("""
        SELECT COALESCE(SUM(b.finalPrice), 0) FROM Booking b
        WHERE b.branch.id = :branchId
        AND b.bookingDate >= :startDate
        AND b.bookingDate <= :endDate
        AND b.paymentStatus = 'PAID'
    """)
    fun getRevenueByBranchIdAndDateRange(
        @Param("branchId") branchId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): BigDecimal
}
