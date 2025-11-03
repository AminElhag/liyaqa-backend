package com.liyaqa.backend.facility.booking.dto

import com.liyaqa.backend.facility.booking.domain.Court
import com.liyaqa.backend.facility.booking.domain.CourtStatus
import com.liyaqa.backend.facility.booking.domain.CourtType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Request to create a new court.
 */
data class CourtCreateRequest(
    @field:NotNull(message = "Facility ID is required")
    val facilityId: UUID,

    @field:NotNull(message = "Branch ID is required")
    val branchId: UUID,

    @field:NotBlank(message = "Court name is required")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Court type is required")
    val courtType: CourtType,

    val surfaceType: String? = null,
    val isIndoor: Boolean = false,
    val hasLighting: Boolean = false,
    val maxPlayers: Int = 4,

    @field:NotNull(message = "Hourly rate is required")
    @field:Min(value = 0, message = "Hourly rate must be positive")
    val hourlyRate: BigDecimal,

    val currency: String = "USD",
    val peakHourRate: BigDecimal? = null,

    val minBookingDuration: Int = 60,
    val maxBookingDuration: Int = 120,
    val bookingInterval: Int = 30,
    val advanceBookingDays: Int = 14,
    val cancellationHours: Int = 24,

    val amenities: String? = null,
    val displayOrder: Int = 0,
    val imageUrl: String? = null
)

/**
 * Request to update court information.
 */
data class CourtUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val surfaceType: String? = null,
    val isIndoor: Boolean? = null,
    val hasLighting: Boolean? = null,
    val maxPlayers: Int? = null,
    val hourlyRate: BigDecimal? = null,
    val currency: String? = null,
    val peakHourRate: BigDecimal? = null,
    val minBookingDuration: Int? = null,
    val maxBookingDuration: Int? = null,
    val bookingInterval: Int? = null,
    val advanceBookingDays: Int? = null,
    val cancellationHours: Int? = null,
    val status: CourtStatus? = null,
    val maintenanceNotes: String? = null,
    val amenities: String? = null,
    val displayOrder: Int? = null,
    val imageUrl: String? = null
)

/**
 * Response DTO for court with full details.
 */
data class CourtResponse(
    val id: UUID,
    val facilityId: UUID,
    val facilityName: String,
    val branchId: UUID,
    val branchName: String,
    val tenantId: String,

    val name: String,
    val description: String?,
    val courtType: CourtType,
    val surfaceType: String?,
    val isIndoor: Boolean,
    val hasLighting: Boolean,
    val maxPlayers: Int,

    val hourlyRate: BigDecimal,
    val currency: String,
    val peakHourRate: BigDecimal?,

    val minBookingDuration: Int,
    val maxBookingDuration: Int,
    val bookingInterval: Int,
    val advanceBookingDays: Int,
    val cancellationHours: Int,

    val status: CourtStatus,
    val maintenanceNotes: String?,
    val amenities: String?,
    val displayOrder: Int,
    val imageUrl: String?,

    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(court: Court): CourtResponse {
            return CourtResponse(
                id = court.id!!,
                facilityId = court.facility.id!!,
                facilityName = court.facility.name,
                branchId = court.branch.id!!,
                branchName = court.branch.name,
                tenantId = court.tenantId,
                name = court.name,
                description = court.description,
                courtType = court.courtType,
                surfaceType = court.surfaceType,
                isIndoor = court.isIndoor,
                hasLighting = court.hasLighting,
                maxPlayers = court.maxPlayers,
                hourlyRate = court.hourlyRate,
                currency = court.currency,
                peakHourRate = court.peakHourRate,
                minBookingDuration = court.minBookingDuration,
                maxBookingDuration = court.maxBookingDuration,
                bookingInterval = court.bookingInterval,
                advanceBookingDays = court.advanceBookingDays,
                cancellationHours = court.cancellationHours,
                status = court.status,
                maintenanceNotes = court.maintenanceNotes,
                amenities = court.amenities,
                displayOrder = court.displayOrder,
                imageUrl = court.imageUrl,
                createdAt = court.createdAt,
                updatedAt = court.updatedAt
            )
        }
    }
}

/**
 * Response DTO for court with basic information (for lists).
 */
data class CourtBasicResponse(
    val id: UUID,
    val name: String,
    val courtType: CourtType,
    val branchId: UUID,
    val branchName: String,
    val isIndoor: Boolean,
    val hasLighting: Boolean,
    val hourlyRate: BigDecimal,
    val currency: String,
    val status: CourtStatus,
    val imageUrl: String?
) {
    companion object {
        fun from(court: Court): CourtBasicResponse {
            return CourtBasicResponse(
                id = court.id!!,
                name = court.name,
                courtType = court.courtType,
                branchId = court.branch.id!!,
                branchName = court.branch.name,
                isIndoor = court.isIndoor,
                hasLighting = court.hasLighting,
                hourlyRate = court.hourlyRate,
                currency = court.currency,
                status = court.status,
                imageUrl = court.imageUrl
            )
        }
    }
}
