package com.liyaqa.backend.internal.facility.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.*

/**
 * Request DTO for creating a new sport facility.
 */
data class FacilityCreateRequest(
    @field:NotBlank(message = "Owner tenant ID is required")
    val ownerTenantId: UUID,

    @field:NotBlank(message = "Facility name is required")
    @field:Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    val name: String,

    val description: String? = null,

    @field:NotBlank(message = "Facility type is required")
    @field:Size(max = 100, message = "Facility type must not exceed 100 characters")
    val facilityType: String,

    @field:NotBlank(message = "Contact email is required")
    @field:Email(message = "Invalid email format")
    val contactEmail: String,

    @field:Size(max = 50, message = "Contact phone must not exceed 50 characters")
    val contactPhone: String? = null,

    @field:Size(max = 255, message = "Website URL must not exceed 255 characters")
    val website: String? = null,

    val socialFacebook: String? = null,
    val socialInstagram: String? = null,
    val socialTwitter: String? = null,

    val establishedDate: LocalDate? = null,
    val registrationNumber: String? = null,

    val amenities: List<String>? = null,
    val operatingHours: String? = null,

    val timezone: String = "UTC",
    val locale: String = "en_US",
    val currency: String = "USD"
)
