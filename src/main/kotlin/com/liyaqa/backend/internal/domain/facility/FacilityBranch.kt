package com.liyaqa.backend.internal.domain.facility

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.domain.employee.Employee
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal

/**
 * Represents a physical branch/location of a sport facility.
 *
 * Each sport facility can have multiple branches representing different
 * physical locations where the facility operates. This supports:
 * - Multi-location clubs (e.g., tennis club with 3 branches across city)
 * - Franchise models (same brand, different locations)
 * - Seasonal locations (e.g., indoor winter, outdoor summer)
 *
 * Examples:
 * - "Elite Tennis Academy - Downtown Branch"
 * - "Beach Volleyball Club - Venice Beach Location"
 * - "Sports Complex - North Campus"
 *
 * Design Philosophy:
 * - Complete address information for each physical location
 * - Geographic coordinates for mapping and distance calculations
 * - Capacity tracking (courts, fields, participants)
 * - Branch-specific amenities and operating hours
 * - Main branch designation for primary location
 */
@Entity
@Table(
    name = "facility_branches",
    indexes = [
        Index(name = "idx_branch_facility", columnList = "facility_id"),
        Index(name = "idx_branch_status", columnList = "status"),
        Index(name = "idx_branch_main", columnList = "is_main_branch"),
        Index(name = "idx_branch_city", columnList = "city"),
        Index(name = "idx_branch_postal_code", columnList = "postal_code"),
        Index(name = "idx_branch_coordinates", columnList = "latitude,longitude")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class FacilityBranch(
    // Foreign Key to Sport Facility
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    // Basic Information
    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "is_main_branch", nullable = false)
    var isMainBranch: Boolean = false,

    // === Address ===
    @Column(name = "address_line1", nullable = false)
    var addressLine1: String,

    @Column(name = "address_line2")
    var addressLine2: String? = null,

    @Column(nullable = false, length = 100)
    var city: String,

    @Column(name = "state_province", length = 100)
    var stateProvince: String? = null,

    @Column(name = "postal_code", nullable = false, length = 20)
    var postalCode: String,

    @Column(nullable = false, length = 100)
    var country: String,

    // === Geographic Coordinates ===
    @Column(precision = 10, scale = 8)
    var latitude: BigDecimal? = null,

    @Column(precision = 11, scale = 8)
    var longitude: BigDecimal? = null,

    // === Contact Information ===
    @Column(name = "contact_email")
    var contactEmail: String? = null,

    @Column(name = "contact_phone", length = 50)
    var contactPhone: String? = null,

    // === Capacity & Facilities ===
    @Column(name = "total_courts")
    var totalCourts: Int = 0,

    @Column(name = "total_capacity")
    var totalCapacity: Int = 0,

    // === Amenities ===
    // JSON or comma-separated list specific to this branch
    @Column(columnDefinition = "TEXT")
    var amenities: String? = null,

    // === Operating Hours ===
    // JSON format: {"monday": "08:00-22:00", "tuesday": "08:00-22:00", ...}
    @Column(name = "operating_hours", columnDefinition = "TEXT")
    var operatingHours: String? = null,

    // === Status ===
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: BranchStatus = BranchStatus.ACTIVE,

    // === Settings ===
    @Column(length = 50)
    var timezone: String = "UTC",

    // === Audit ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null

) : BaseEntity() {

    /**
     * Check if branch is operational.
     */
    fun isOperational(): Boolean {
        return status == BranchStatus.ACTIVE
    }

    /**
     * Check if branch can accept bookings.
     */
    fun canAcceptBookings(): Boolean {
        return status == BranchStatus.ACTIVE && totalCourts > 0
    }

    /**
     * Check if branch has geographic coordinates.
     */
    fun hasCoordinates(): Boolean {
        return latitude != null && longitude != null
    }

    /**
     * Activate branch.
     */
    fun activate() {
        this.status = BranchStatus.ACTIVE
    }

    /**
     * Deactivate branch.
     */
    fun deactivate() {
        this.status = BranchStatus.INACTIVE
    }

    /**
     * Mark branch as under renovation.
     */
    fun markUnderRenovation() {
        this.status = BranchStatus.UNDER_RENOVATION
    }

    /**
     * Close branch temporarily.
     */
    fun closeTemporarily() {
        this.status = BranchStatus.TEMPORARILY_CLOSED
    }

    /**
     * Close branch permanently.
     */
    fun closePermanently() {
        this.status = BranchStatus.PERMANENTLY_CLOSED
    }

    /**
     * Get full address as single string.
     */
    fun getFullAddress(): String {
        val parts = mutableListOf(addressLine1)
        addressLine2?.let { parts.add(it) }
        parts.add(city)
        stateProvince?.let { parts.add(it) }
        parts.add(postalCode)
        parts.add(country)
        return parts.joinToString(", ")
    }

    /**
     * Parse amenities from JSON/CSV string to list.
     */
    fun getAmenitiesList(): List<String> {
        return amenities?.split(",")?.map { it.trim() } ?: emptyList()
    }

    /**
     * Set amenities from list.
     */
    fun setAmenitiesList(amenitiesList: List<String>) {
        this.amenities = amenitiesList.joinToString(",")
    }

    /**
     * Get coordinates as pair.
     */
    fun getCoordinates(): Pair<BigDecimal, BigDecimal>? {
        return if (latitude != null && longitude != null) {
            Pair(latitude!!, longitude!!)
        } else {
            null
        }
    }

    /**
     * Get display name for UI.
     */
    fun getDisplayName(): String {
        return if (isMainBranch) {
            "$name (Main)"
        } else {
            name
        }
    }

    override fun toString(): String {
        return "FacilityBranch(id=$id, tenantId='$tenantId', name='$name', city='$city', status=$status, isMain=$isMainBranch)"
    }
}
