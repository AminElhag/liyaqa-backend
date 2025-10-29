package com.liyaqa.backend.internal.dto.facility

import com.liyaqa.backend.internal.domain.facility.FacilityStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * Request DTO for updating an existing sport facility.
 * All fields are optional - only provided fields will be updated.
 */
data class FacilityUpdateRequest(
    @field:Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    val name: String? = null,

    val description: String? = null,

    @field:Size(max = 100, message = "Facility type must not exceed 100 characters")
    val facilityType: String? = null,

    @field:Email(message = "Invalid email format")
    val contactEmail: String? = null,

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

    val status: FacilityStatus? = null,

    val timezone: String? = null,
    val locale: String? = null,
    val currency: String? = null
)
