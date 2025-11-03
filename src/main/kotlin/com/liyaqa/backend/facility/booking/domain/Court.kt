package com.liyaqa.backend.facility.booking.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.facility.domain.FacilityBranch
import com.liyaqa.backend.internal.facility.domain.SportFacility
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal

/**
 * Court or resource that can be booked at a facility branch.
 *
 * Examples: Tennis Court 1, Padel Court A, Basketball Court, etc.
 */
@Entity
@Table(
    name = "courts",
    indexes = [
        Index(name = "idx_court_branch", columnList = "branch_id"),
        Index(name = "idx_court_facility", columnList = "facility_id"),
        Index(name = "idx_court_type", columnList = "court_type"),
        Index(name = "idx_court_status", columnList = "status"),
        Index(name = "idx_court_tenant", columnList = "tenant_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_branch_court_name", columnNames = ["branch_id", "name"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Court(
    // Facility and Branch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    var branch: FacilityBranch,

    // Basic Information
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "court_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var courtType: CourtType,

    // Specifications
    @Column(name = "surface_type", length = 50)
    var surfaceType: String? = null, // Clay, Grass, Hard, Artificial, etc.

    @Column(name = "is_indoor")
    var isIndoor: Boolean = false,

    @Column(name = "has_lighting")
    var hasLighting: Boolean = false,

    // Capacity
    @Column(name = "max_players")
    var maxPlayers: Int = 4,

    // Pricing
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    var hourlyRate: BigDecimal,

    @Column(length = 3)
    var currency: String = "USD",

    @Column(name = "peak_hour_rate", precision = 10, scale = 2)
    var peakHourRate: BigDecimal? = null,

    // Booking Settings
    @Column(name = "min_booking_duration")
    var minBookingDuration: Int = 60, // minutes

    @Column(name = "max_booking_duration")
    var maxBookingDuration: Int = 120, // minutes

    @Column(name = "booking_interval")
    var bookingInterval: Int = 30, // minutes - time slot intervals

    @Column(name = "advance_booking_days")
    var advanceBookingDays: Int = 14, // how far in advance can book

    @Column(name = "cancellation_hours")
    var cancellationHours: Int = 24, // hours before start time

    // Status
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: CourtStatus = CourtStatus.ACTIVE,

    // Maintenance
    @Column(name = "maintenance_notes", columnDefinition = "TEXT")
    var maintenanceNotes: String? = null,

    // Amenities
    @Column(columnDefinition = "TEXT")
    var amenities: String? = null, // JSON or comma-separated

    // Display
    @Column(name = "display_order")
    var displayOrder: Int = 0,

    @Column(name = "image_url", length = 500)
    var imageUrl: String? = null

) : BaseEntity() {

    // Multi-tenancy field
    @Column(name = "tenant_id", length = 100, nullable = false)
    override lateinit var tenantId: String

    /**
     * Check if court is available for booking.
     */
    fun isAvailableForBooking(): Boolean {
        return status == CourtStatus.ACTIVE
    }

    /**
     * Check if court is indoor.
     */
    fun isIndoorCourt(): Boolean {
        return isIndoor
    }

    /**
     * Get display name with type.
     */
    fun getDisplayName(): String {
        return "$name (${courtType.displayName})"
    }

    override fun toString(): String {
        return "Court(id=$id, name='$name', type=$courtType, branch=${branch.name}, status=$status)"
    }
}

/**
 * Court type enum.
 */
enum class CourtType(val displayName: String) {
    TENNIS("Tennis Court"),
    PADEL("Padel Court"),
    SQUASH("Squash Court"),
    BADMINTON("Badminton Court"),
    BASKETBALL("Basketball Court"),
    VOLLEYBALL("Volleyball Court"),
    FOOTBALL("Football Field"),
    MULTIPURPOSE("Multi-Purpose Court")
}

/**
 * Court status enum.
 */
enum class CourtStatus {
    ACTIVE,       // Available for booking
    MAINTENANCE,  // Under maintenance, not bookable
    INACTIVE,     // Temporarily inactive
    RETIRED       // Permanently retired
}
