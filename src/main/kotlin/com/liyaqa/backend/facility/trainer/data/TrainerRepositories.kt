package com.liyaqa.backend.facility.trainer.data

import com.liyaqa.backend.facility.trainer.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Repository for Trainer entity.
 */
@Repository
interface TrainerRepository : JpaRepository<Trainer, UUID> {

    fun findByFacility_IdAndStatusOrderByFirstNameAsc(facilityId: UUID, status: TrainerStatus): List<Trainer>

    fun findByBranch_IdAndStatusOrderByFirstNameAsc(branchId: UUID, status: TrainerStatus): List<Trainer>

    fun findByTenantIdAndStatusOrderByFirstNameAsc(tenantId: String, status: TrainerStatus): List<Trainer>

    fun findByEmailAndTenantId(email: String, tenantId: String): Trainer?

    @Query("""
        SELECT t FROM Trainer t
        WHERE t.facility.id = :facilityId
        AND t.status = 'ACTIVE'
        AND t.availableForBooking = true
        ORDER BY t.averageRating DESC, t.totalSessions DESC
    """)
    fun findAvailableTrainersByFacility(@Param("facilityId") facilityId: UUID): List<Trainer>

    fun countByFacility_IdAndStatus(facilityId: UUID, status: TrainerStatus): Long
}

/**
 * Repository for TrainerAvailability entity.
 */
@Repository
interface TrainerAvailabilityRepository : JpaRepository<TrainerAvailability, UUID> {

    fun findByTrainer_IdAndIsActiveTrueOrderByDayOfWeekAsc(trainerId: UUID): List<TrainerAvailability>

    fun findByTrainer_IdAndAvailabilityTypeAndIsActiveTrue(
        trainerId: UUID,
        availabilityType: AvailabilityType
    ): List<TrainerAvailability>

    @Query("""
        SELECT ta FROM TrainerAvailability ta
        WHERE ta.trainer.id = :trainerId
        AND ta.availabilityType = 'REGULAR'
        AND ta.dayOfWeek = :dayOfWeek
        AND ta.isActive = true
    """)
    fun findRegularAvailabilityByDay(
        @Param("trainerId") trainerId: UUID,
        @Param("dayOfWeek") dayOfWeek: DayOfWeek
    ): List<TrainerAvailability>

    @Query("""
        SELECT ta FROM TrainerAvailability ta
        WHERE ta.trainer.id = :trainerId
        AND ta.availabilityType = 'TIME_OFF'
        AND ta.specificDate = :date
        AND ta.isActive = true
    """)
    fun findTimeOffByDate(
        @Param("trainerId") trainerId: UUID,
        @Param("date") date: LocalDate
    ): List<TrainerAvailability>
}

/**
 * Repository for TrainerBooking entity.
 */
@Repository
interface TrainerBookingRepository : JpaRepository<TrainerBooking, UUID> {

    fun findByBookingNumber(bookingNumber: String): TrainerBooking?

    fun findByMember_IdOrderBySessionDateDescStartTimeDesc(memberId: UUID, pageable: Pageable): Page<TrainerBooking>

    fun findByTrainer_IdOrderBySessionDateDescStartTimeDesc(trainerId: UUID, pageable: Pageable): Page<TrainerBooking>

    @Query("""
        SELECT tb FROM TrainerBooking tb
        WHERE tb.trainer.id = :trainerId
        AND tb.sessionDate = :date
        AND tb.status NOT IN ('CANCELLED', 'RESCHEDULED')
        ORDER BY tb.startTime ASC
    """)
    fun findByTrainerAndDate(
        @Param("trainerId") trainerId: UUID,
        @Param("date") date: LocalDate
    ): List<TrainerBooking>

    @Query("""
        SELECT tb FROM TrainerBooking tb
        WHERE tb.trainer.id = :trainerId
        AND tb.sessionDate BETWEEN :startDate AND :endDate
        AND tb.status NOT IN ('CANCELLED', 'RESCHEDULED')
    """)
    fun findByTrainerAndDateRange(
        @Param("trainerId") trainerId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<TrainerBooking>

    @Query("""
        SELECT COUNT(tb) FROM TrainerBooking tb
        WHERE tb.trainer.id = :trainerId
        AND tb.startTime > :startTime
        AND tb.endTime < :endTime
        AND tb.status NOT IN ('CANCELLED', 'RESCHEDULED')
    """)
    fun countConflictingBookings(
        @Param("trainerId") trainerId: UUID,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): Long

    fun countByTrainer_IdAndStatus(trainerId: UUID, status: TrainerBookingStatus): Long

    fun countByMember_IdAndStatus(memberId: UUID, status: TrainerBookingStatus): Long
}

/**
 * Repository for TrainerReview entity.
 */
@Repository
interface TrainerReviewRepository : JpaRepository<TrainerReview, UUID> {

    fun findByTrainer_IdAndStatusOrderByCreatedAtDesc(
        trainerId: UUID,
        status: ReviewStatus,
        pageable: Pageable
    ): Page<TrainerReview>

    fun findByMember_IdOrderByCreatedAtDesc(memberId: UUID): List<TrainerReview>

    fun findByBooking_Id(bookingId: UUID): TrainerReview?

    @Query("""
        SELECT AVG(tr.rating) FROM TrainerReview tr
        WHERE tr.trainer.id = :trainerId
        AND tr.status = 'APPROVED'
    """)
    fun getAverageRatingForTrainer(@Param("trainerId") trainerId: UUID): Double?

    fun countByTrainer_IdAndStatus(trainerId: UUID, status: ReviewStatus): Long
}
