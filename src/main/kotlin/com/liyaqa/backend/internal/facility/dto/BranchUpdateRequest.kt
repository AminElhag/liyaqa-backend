package com.liyaqa.backend.internal.facility.dto

import com.liyaqa.backend.internal.facility.domain.BranchStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * Request DTO for updating an existing facility branch.
 * All fields are optional - only provided fields will be updated.
 */
data class BranchUpdateRequest(
    @field:Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    val name: String? = null,

    val description: String? = null,

    val isMainBranch: Boolean? = null,

    // Address
    @field:Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    val addressLine1: String? = null,

    @field:Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    val addressLine2: String? = null,

    @field:Size(max = 100, message = "City must not exceed 100 characters")
    val city: String? = null,

    @field:Size(max = 100, message = "State/Province must not exceed 100 characters")
    val stateProvince: String? = null,

    @field:Size(max = 20, message = "Postal code must not exceed 20 characters")
    val postalCode: String? = null,

    @field:Size(max = 100, message = "Country must not exceed 100 characters")
    val country: String? = null,

    // Geographic coordinates
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,

    // Contact
    @field:Email(message = "Invalid email format")
    val contactEmail: String? = null,

    @field:Size(max = 50, message = "Contact phone must not exceed 50 characters")
    val contactPhone: String? = null,

    // Capacity
    val totalCourts: Int? = null,
    val totalCapacity: Int? = null,

    // Amenities and hours
    val amenities: List<String>? = null,
    val operatingHours: String? = null,

    val status: BranchStatus? = null,

    val timezone: String? = null
)
