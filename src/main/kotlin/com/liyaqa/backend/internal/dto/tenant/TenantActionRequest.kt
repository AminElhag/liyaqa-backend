package com.liyaqa.backend.internal.dto.tenant

import jakarta.validation.constraints.NotBlank

/**
 * Request to suspend a tenant.
 */
data class SuspendTenantRequest(
    @field:NotBlank(message = "Suspension reason is required")
    val reason: String
)

/**
 * Request to accept terms and conditions for a tenant.
 */
data class AcceptTermsRequest(
    @field:NotBlank(message = "Accepted by name is required")
    val acceptedBy: String,

    @field:NotBlank(message = "Terms version is required")
    val termsVersion: String
)

/**
 * Request to change subscription plan.
 */
data class ChangePlanRequest(
    val newPlanTier: com.liyaqa.backend.internal.domain.tenant.PlanTier
)
