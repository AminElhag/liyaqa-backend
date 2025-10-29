package com.liyaqa.backend.internal.dto.facility

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.*

/**
 * Request DTO for creating a new facility branch.
 */
data class BranchCreateRequest(
    @field:NotBlank(message = "Facility ID is required")
    val facilityId: UUID,

    @field:NotBlank(message = "Branch name is required")
    @field:Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    val name: String,

    val description: String? = null,

    val isMainBranch: Boolean = false,

    // Address
    @field:NotBlank(message = "Address line 1 is required")
    @field:Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    val addressLine1: String,

    @field:Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    val addressLine2: String? = null,

    @field:NotBlank(message = "City is required")
    @field:Size(max = 100, message = "City must not exceed 100 characters")
    val city: String,

    @field:Size(max = 100, message = "State/Province must not exceed 100 characters")
    val stateProvince: String? = null,

    @field:NotBlank(message = "Postal code is required")
    @field:Size(max = 20, message = "Postal code must not exceed 20 characters")
    val postalCode: String,

    @field:NotBlank(message = "Country is required")
    @field:Size(max = 100, message = "Country must not exceed 100 characters")
    val country: String,

    // Geographic coordinates
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,

    // Contact
    @field:Email(message = "Invalid email format")
    val contactEmail: String? = null,

    @field:Size(max = 50, message = "Contact phone must not exceed 50 characters")
    val contactPhone: String? = null,

    // Capacity
    val totalCourts: Int = 0,
    val totalCapacity: Int = 0,

    // Amenities and hours
    val amenities: List<String>? = null,
    val operatingHours: String? = null,

    val timezone: String = "UTC"
)
