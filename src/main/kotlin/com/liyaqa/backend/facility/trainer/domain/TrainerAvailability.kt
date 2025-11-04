package com.liyaqa.backend.facility.trainer.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.*

/**
 * Trainer availability schedule.
 *
 * Design Philosophy:
 * "Flexibility in scheduling creates opportunities for connection."
 *
 * Manages trainer working hours and availability patterns:
 * - Regular weekly schedule
 * - One-time availability blocks
 * - Time-off/unavailable periods
 */
@Entity
@Table(
    name = "trainer_availabilities",
    indexes = [
        Index(name = "idx_trainer_avail_trainer", columnList = "trainer_id"),
        Index(name = "idx_trainer_avail_day", columnList = "day_of_week"),
        Index(name = "idx_trainer_avail_type", columnList = "availability_type"),
        Index(name = "idx_trainer_avail_tenant", columnList = "tenant_id")
    ]
)
class TrainerAvailability(
    // Relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    var trainer: Trainer,

    // Availability Type
    @Enumerated(EnumType.STRING)
    @Column(name = "availability_type", nullable = false, length = 50)
    var availabilityType: AvailabilityType,

    // For REGULAR schedule
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 20)
    var dayOfWeek: DayOfWeek? = null,

    @Column(name = "start_time")
    var startTime: LocalTime? = null,

    @Column(name = "end_time")
    var endTime: LocalTime? = null,

    // For ONE_TIME or TIME_OFF
    @Column(name = "specific_date")
    var specificDate: java.time.LocalDate? = null,

    @Column(name = "specific_start_datetime")
    var specificStartDateTime: java.time.LocalDateTime? = null,

    @Column(name = "specific_end_datetime")
    var specificEndDateTime: java.time.LocalDateTime? = null,

    // Status
    @Column(name = "is_active")
    var isActive: Boolean = true,

    // Notes
    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null

) : BaseEntity() {

    /**
     * Check if this availability conflicts with another.
     */
    fun conflictsWith(other: TrainerAvailability): Boolean {
        // Check for same day/time conflict
        if (availabilityType == AvailabilityType.REGULAR && other.availabilityType == AvailabilityType.REGULAR) {
            if (dayOfWeek == other.dayOfWeek) {
                return timesOverlap(startTime, endTime, other.startTime, other.endTime)
            }
        }

        // Check for specific datetime conflict
        if (specificStartDateTime != null && other.specificStartDateTime != null) {
            return specificStartDateTime!!.isBefore(other.specificEndDateTime) &&
                    specificEndDateTime!!.isAfter(other.specificStartDateTime)
        }

        return false
    }

    private fun timesOverlap(start1: LocalTime?, end1: LocalTime?, start2: LocalTime?, end2: LocalTime?): Boolean {
        if (start1 == null || end1 == null || start2 == null || end2 == null) return false
        return start1.isBefore(end2) && end1.isAfter(start2)
    }
}

/**
 * Availability type.
 */
enum class AvailabilityType {
    REGULAR,     // Regular weekly schedule (e.g., Monday 9am-5pm)
    ONE_TIME,    // One-time availability (e.g., special weekend session)
    TIME_OFF     // Unavailable period (vacation, sick leave, etc.)
}
