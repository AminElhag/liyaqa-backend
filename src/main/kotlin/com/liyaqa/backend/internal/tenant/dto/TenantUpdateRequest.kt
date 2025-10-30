package com.liyaqa.backend.internal.tenant.dto

import com.liyaqa.backend.internal.tenant.domain.PlanTier
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * Request to update an existing tenant.
 *
 * All fields are optional - only provided fields will be updated.
 * This supports partial updates for flexibility.
 */
data class TenantUpdateRequest(
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String? = null,

    // Contact Information
    @field:Email(message = "Contact email must be valid")
    val contactEmail: String? = null,

    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in international format"
    )
    val contactPhone: String? = null,

    val contactPerson: String? = null,

    // Billing Information
    @field:Email(message = "Billing email must be valid")
    val billingEmail: String? = null,

    val billingAddress: String? = null,

    val taxId: String? = null,

    // Subscription
    val planTier: PlanTier? = null,

    // Contract
    val contractStartDate: LocalDate? = null,
    val contractEndDate: LocalDate? = null,

    // Additional Info
    val description: String? = null,
    val facilityType: String? = null,
    val timezone: String? = null,
    val locale: String? = null
)
