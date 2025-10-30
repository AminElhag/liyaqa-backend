package com.liyaqa.backend.internal.facility.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.tenant.domain.Tenant
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate

/**
 * Represents a sport facility (club) owned by a tenant.
 *
 * A sport facility is the main entity representing a sports club or complex.
 * Each tenant can own multiple sport facilities, and each facility can have
 * multiple physical branches (locations).
 *
 * Examples:
 * - "Elite Tennis Academy" (Tennis facility with 3 branches)
 * - "Downtown Sports Complex" (Multi-sport facility with main location)
 * - "Beach Volleyball Club" (Volleyball facility with 2 beach locations)
 *
 * Design Philosophy:
 * - Separation from Tenant (organizational) vs Facility (operational)
 * - Support for multi-location facilities via branches
 * - Rich metadata for marketing (social media, website, amenities)
 * - Flexible facility types for different sports
 */
@Entity
@Table(
    name = "sport_facilities",
    indexes = [
        Index(name = "idx_facility_tenant", columnList = "owner_tenant_id"),
        Index(name = "idx_facility_status", columnList = "status"),
        Index(name = "idx_facility_type", columnList = "facility_type"),
        Index(name = "idx_facility_name", columnList = "name")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class SportFacility(
    // Foreign Key to owning Tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_tenant_id", nullable = false)
    var owner: Tenant,

    // Basic Information
    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    // Facility Type (Tennis, Basketball, Multi-Sport, Gym, etc.)
    @Column(name = "facility_type", nullable = false, length = 100)
    var facilityType: String,

    // === Contact Information ===
    @Column(name = "contact_email", nullable = false)
    var contactEmail: String,

    @Column(name = "contact_phone", length = 50)
    var contactPhone: String? = null,

    @Column(length = 255)
    var website: String? = null,

    // === Social Media ===
    @Column(name = "social_facebook", length = 255)
    var socialFacebook: String? = null,

    @Column(name = "social_instagram", length = 255)
    var socialInstagram: String? = null,

    @Column(name = "social_twitter", length = 255)
    var socialTwitter: String? = null,

    // === Business Information ===
    @Column(name = "established_date")
    var establishedDate: LocalDate? = null,

    @Column(name = "registration_number", length = 100)
    var registrationNumber: String? = null,

    // === Amenities ===
    // JSON or comma-separated list: "parking,locker_rooms,cafe,pro_shop,wifi"
    @Column(columnDefinition = "TEXT")
    var amenities: String? = null,

    // === Operating Hours ===
    // JSON format: {"monday": "08:00-22:00", "tuesday": "08:00-22:00", ...}
    @Column(name = "operating_hours", columnDefinition = "TEXT")
    var operatingHours: String? = null,

    // === Status ===
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: FacilityStatus = FacilityStatus.ACTIVE,

    // === Settings ===
    @Column(length = 50)
    var timezone: String = "UTC",

    @Column(length = 10)
    var locale: String = "en_US",

    @Column(length = 3)
    var currency: String = "USD",

    // === Audit ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null

) : BaseEntity() {

    /**
     * Check if facility is operational.
     */
    fun isOperational(): Boolean {
        return status == FacilityStatus.ACTIVE
    }

    /**
     * Check if facility can accept bookings.
     */
    fun canAcceptBookings(): Boolean {
        return status == FacilityStatus.ACTIVE
    }

    /**
     * Activate facility.
     */
    fun activate() {
        this.status = FacilityStatus.ACTIVE
    }

    /**
     * Deactivate facility temporarily.
     */
    fun deactivate() {
        this.status = FacilityStatus.INACTIVE
    }

    /**
     * Mark facility as under maintenance.
     */
    fun markUnderMaintenance() {
        this.status = FacilityStatus.UNDER_MAINTENANCE
    }

    /**
     * Close facility permanently.
     */
    fun close() {
        this.status = FacilityStatus.CLOSED
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
     * Get social media links as map.
     */
    fun getSocialMediaLinks(): Map<String, String?> {
        return mapOf(
            "facebook" to socialFacebook,
            "instagram" to socialInstagram,
            "twitter" to socialTwitter
        )
    }

    /**
     * Get display name for UI.
     */
    fun getDisplayName(): String {
        return name
    }

    override fun toString(): String {
        return "SportFacility(id=$id, tenantId='$tenantId', name='$name', type='$facilityType', status=$status)"
    }
}
