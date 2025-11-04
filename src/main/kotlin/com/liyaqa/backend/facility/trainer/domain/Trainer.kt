package com.liyaqa.backend.facility.trainer.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.facility.domain.SportFacility
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Personal Trainer entity.
 *
 * Design Philosophy:
 * "Great trainers don't just teach technique, they build confidence and relationships."
 *
 * A trainer is a professional service provider within a facility who offers:
 * - Personal training sessions
 * - Group classes
 * - Specialized coaching
 * - Fitness assessments
 */
@Entity
@Table(
    name = "trainers",
    indexes = [
        Index(name = "idx_trainer_facility", columnList = "facility_id"),
        Index(name = "idx_trainer_branch", columnList = "branch_id"),
        Index(name = "idx_trainer_status", columnList = "status"),
        Index(name = "idx_trainer_tenant", columnList = "tenant_id"),
        Index(name = "idx_trainer_email", columnList = "email")
    ]
)
class Trainer(
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    var branch: FacilityBranch,

    // Personal Information
    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String,

    @Column(name = "email", nullable = false, length = 255)
    var email: String,

    @Column(name = "phone_number", length = 20)
    var phoneNumber: String,

    @Column(name = "date_of_birth")
    var dateOfBirth: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    var gender: Gender? = null,

    // Profile
    @Column(name = "bio", columnDefinition = "TEXT")
    var bio: String? = null,

    @Column(name = "profile_photo_url", length = 500)
    var profilePhotoUrl: String? = null,

    @Column(name = "years_of_experience")
    var yearsOfExperience: Int? = null,

    // Specializations
    @Column(name = "specializations", columnDefinition = "TEXT")
    var specializations: String? = null, // JSON array: ["Strength Training", "Yoga", "HIIT", etc.]

    @Column(name = "certifications", columnDefinition = "TEXT")
    var certifications: String? = null, // JSON array of certification objects

    @Column(name = "languages", length = 255)
    var languages: String? = null, // Comma-separated: "English,Spanish,French"

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: TrainerStatus = TrainerStatus.ACTIVE,

    @Column(name = "available_for_booking")
    var availableForBooking: Boolean = true,

    // Pricing
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    var hourlyRate: BigDecimal? = null,

    @Column(name = "session_rate_30min", precision = 10, scale = 2)
    var sessionRate30Min: BigDecimal? = null,

    @Column(name = "session_rate_60min", precision = 10, scale = 2)
    var sessionRate60Min: BigDecimal? = null,

    @Column(name = "session_rate_90min", precision = 10, scale = 2)
    var sessionRate90Min: BigDecimal? = null,

    @Column(name = "currency", length = 3)
    var currency: String = "USD",

    // Ratings & Reviews
    @Column(name = "average_rating", precision = 3, scale = 2)
    var averageRating: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_reviews")
    var totalReviews: Int = 0,

    @Column(name = "total_sessions")
    var totalSessions: Int = 0,

    // Availability Settings
    @Column(name = "min_booking_notice_hours")
    var minBookingNoticeHours: Int = 24, // Minimum hours notice required for booking

    @Column(name = "max_advance_booking_days")
    var maxAdvanceBookingDays: Int = 30, // Maximum days in advance for booking

    @Column(name = "session_buffer_minutes")
    var sessionBufferMinutes: Int = 15, // Buffer time between sessions

    // Employment
    @Column(name = "employee_number", length = 50)
    var employeeNumber: String? = null,

    @Column(name = "hire_date")
    var hireDate: Instant? = null,

    @Column(name = "employment_type", length = 50)
    var employmentType: String? = null, // "FULL_TIME", "PART_TIME", "CONTRACTOR"

    // Social Links
    @Column(name = "instagram_handle", length = 100)
    var instagramHandle: String? = null,

    @Column(name = "website_url", length = 500)
    var websiteUrl: String? = null,

    // Emergency Contact
    @Column(name = "emergency_contact_name", length = 255)
    var emergencyContactName: String? = null,

    @Column(name = "emergency_contact_phone", length = 20)
    var emergencyContactPhone: String? = null

) : BaseEntity() {

    /**
     * Get full name.
     */
    fun getFullName(): String = "$firstName $lastName"

    /**
     * Check if trainer is available for new bookings.
     */
    fun isAvailable(): Boolean = status == TrainerStatus.ACTIVE && availableForBooking

    /**
     * Get rate for session duration.
     */
    fun getRateForDuration(durationMinutes: Int): BigDecimal? {
        return when (durationMinutes) {
            30 -> sessionRate30Min
            60 -> sessionRate60Min
            90 -> sessionRate90Min
            else -> hourlyRate?.multiply(BigDecimal(durationMinutes / 60.0))
        }
    }

    /**
     * Update average rating.
     */
    fun updateRating(newRating: BigDecimal) {
        val totalRatingPoints = averageRating.multiply(BigDecimal(totalReviews))
        totalReviews++
        averageRating = totalRatingPoints.add(newRating).divide(BigDecimal(totalReviews), 2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Increment session count.
     */
    fun incrementSessionCount() {
        totalSessions++
    }
}

/**
 * Trainer status.
 */
enum class TrainerStatus {
    ACTIVE,      // Available and accepting bookings
    INACTIVE,    // Temporarily unavailable
    ON_LEAVE,    // On leave/vacation
    SUSPENDED,   // Suspended by admin
    TERMINATED   // No longer employed
}

/**
 * Gender enum.
 */
enum class Gender {
    MALE,
    FEMALE,
    NON_BINARY,
    PREFER_NOT_TO_SAY
}
