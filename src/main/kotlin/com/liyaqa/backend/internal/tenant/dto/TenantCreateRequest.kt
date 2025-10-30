package com.liyaqa.backend.internal.tenant.dto

import com.liyaqa.backend.internal.tenant.domain.PlanTier
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * Request to create a new tenant (sports facility organization).
 *
 * This represents the onboarding data for a new customer.
 * Validation ensures data quality and prevents common errors.
 */
data class TenantCreateRequest(
    @field:NotBlank(message = "Tenant ID is required")
    @field:Pattern(
        regexp = "^[a-z0-9-]+$",
        message = "Tenant ID must be lowercase alphanumeric with hyphens only"
    )
    @field:Size(min = 3, max = 100, message = "Tenant ID must be between 3 and 100 characters")
    val tenantId: String,

    @field:NotBlank(message = "Organization name is required")
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String,

    // Contact Information
    @field:NotBlank(message = "Contact email is required")
    @field:Email(message = "Contact email must be valid")
    val contactEmail: String,

    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone number must be in international format"
    )
    val contactPhone: String? = null,

    val contactPerson: String? = null,

    // Billing Information
    @field:NotBlank(message = "Billing email is required")
    @field:Email(message = "Billing email must be valid")
    val billingEmail: String,

    val billingAddress: String? = null,

    val taxId: String? = null,

    // Subscription
    val planTier: PlanTier = PlanTier.FREE,

    // Multi-tenancy
    @field:Pattern(
        regexp = "^[a-z0-9-]*$",
        message = "Subdomain must be lowercase alphanumeric with hyphens only"
    )
    @field:Size(max = 100, message = "Subdomain must not exceed 100 characters")
    val subdomain: String? = null,

    // Contract
    val contractStartDate: LocalDate? = null,
    val contractEndDate: LocalDate? = null,

    // Additional Info
    val description: String? = null,
    val facilityType: String? = null,
    val timezone: String = "UTC",
    val locale: String = "en_US"
)
