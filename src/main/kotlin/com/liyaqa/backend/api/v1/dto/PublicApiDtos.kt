package com.liyaqa.backend.api.v1.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Public API v1 DTOs.
 *
 * Design Principles:
 * - Stable contract (breaking changes require new version)
 * - Minimal exposure (only public-facing data)
 * - Clear documentation
 * - Consistent naming
 */

// ========== Facility DTOs ==========

data class PublicFacilityResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val facilityType: String,
    val contactEmail: String,
    val contactPhone: String?,
    val website: String?,
    val timezone: String,
    val currency: String
)

data class PublicBranchResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val address: PublicAddressResponse,
    val contactEmail: String?,
    val contactPhone: String?,
    val totalCourts: Int,
    val amenities: List<String>,
    val operatingHours: Map<String, String>?
)

data class PublicAddressResponse(
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val postalCode: String,
    val country: String,
    val latitude: Double?,
    val longitude: Double?
)

// ========== Court DTOs ==========

data class PublicCourtResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val courtType: String,
    val surfaceType: String?,
    val isIndoor: Boolean,
    val hasLighting: Boolean,
    val maxPlayers: Int,
    val hourlyRate: BigDecimal,
    val currency: String,
    val amenities: List<String>,
    val status: String
)

data class CourtAvailabilityRequest(
    val courtId: UUID?,
    val date: Instant,
    val durationMinutes: Int = 60
)

data class CourtAvailabilityResponse(
    val courtId: UUID,
    val courtName: String,
    val date: Instant,
    val availableSlots: List<TimeSlot>
)

data class TimeSlot(
    val startTime: Instant,
    val endTime: Instant,
    val available: Boolean,
    val price: BigDecimal
)

// ========== Booking DTOs ==========

data class CreateBookingRequest(
    val courtId: UUID,
    val startTime: Instant,
    val endTime: Instant,
    val memberId: UUID?,
    val memberEmail: String?,
    val memberName: String?,
    val numberOfPlayers: Int = 2,
    val specialRequests: String?
)

data class PublicBookingResponse(
    val id: UUID,
    val bookingNumber: String,
    val court: PublicCourtResponse,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Int,
    val status: String,
    val totalPrice: BigDecimal,
    val currency: String,
    val member: PublicMemberResponse?,
    val createdAt: Instant
)

data class CancelBookingRequest(
    val reason: String?
)

// ========== Member DTOs ==========

data class CreateMemberRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val dateOfBirth: Instant?,
    val marketingConsent: Boolean = false
)

data class UpdateMemberRequest(
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val marketingConsent: Boolean?
)

data class PublicMemberResponse(
    val id: UUID,
    val memberNumber: String?,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val status: String,
    val createdAt: Instant
)

// ========== Payment DTOs ==========

data class CreatePaymentIntentRequest(
    val bookingId: UUID,
    val amount: BigDecimal,
    val currency: String = "USD",
    val paymentMethod: String = "card"
)

data class PublicPaymentResponse(
    val id: UUID,
    val transactionNumber: String,
    val amount: BigDecimal,
    val currency: String,
    val status: String,
    val paymentMethod: String?,
    val createdAt: Instant,
    val clientSecret: String? // For frontend payment completion
)

// ========== Common DTOs ==========

data class ApiErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val path: String? = null
)

data class ApiSuccessResponse<T>(
    val success: Boolean = true,
    val data: T,
    val timestamp: Instant = Instant.now()
)

data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
