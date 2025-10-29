package com.liyaqa.backend.internal.dto.tenant

import com.liyaqa.backend.internal.domain.tenant.PlanTier
import com.liyaqa.backend.internal.domain.tenant.SubscriptionStatus
import com.liyaqa.backend.internal.domain.tenant.Tenant
import com.liyaqa.backend.internal.domain.tenant.TenantStatus
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Public representation of a tenant for API responses.
 *
 * Excludes internal fields and formats data appropriately.
 * This is what clients see when querying tenant information.
 */
data class TenantResponse(
    val id: UUID,
    val tenantId: String,
    val name: String,
    val status: TenantStatus,
    val subscriptionStatus: SubscriptionStatus,
    val planTier: PlanTier,

    // Contact
    val contactEmail: String,
    val contactPhone: String?,
    val contactPerson: String?,

    // Billing
    val billingEmail: String,
    val billingAddress: String?,
    val taxId: String?,

    // Multi-tenancy
    val subdomain: String?,
    val tenantUrl: String?,

    // Contract
    val contractStartDate: LocalDate?,
    val contractEndDate: LocalDate?,
    val termsAcceptedAt: Instant?,
    val termsVersion: String?,

    // Metadata
    val description: String?,
    val facilityType: String?,
    val timezone: String,
    val locale: String,

    // Status tracking
    val suspendedAt: Instant?,
    val suspensionReason: String?,

    // Audit
    val createdAt: Instant,
    val updatedAt: Instant?,
    val createdByName: String?
) {
    companion object {
        /**
         * Convert tenant entity to response DTO.
         */
        fun from(tenant: Tenant): TenantResponse {
            return TenantResponse(
                id = tenant.id!!,
                tenantId = tenant.tenantId,
                name = tenant.name,
                status = tenant.status,
                subscriptionStatus = tenant.subscriptionStatus,
                planTier = tenant.planTier,

                contactEmail = tenant.contactEmail,
                contactPhone = tenant.contactPhone,
                contactPerson = tenant.contactPerson,

                billingEmail = tenant.billingEmail,
                billingAddress = tenant.billingAddress,
                taxId = tenant.taxId,

                subdomain = tenant.subdomain,
                tenantUrl = tenant.getTenantUrl(),

                contractStartDate = tenant.contractStartDate,
                contractEndDate = tenant.contractEndDate,
                termsAcceptedAt = tenant.termsAcceptedAt,
                termsVersion = tenant.termsVersion,

                description = tenant.description,
                facilityType = tenant.facilityType,
                timezone = tenant.timezone,
                locale = tenant.locale,

                suspendedAt = tenant.suspendedAt,
                suspensionReason = tenant.suspensionReason,

                createdAt = tenant.createdAt!!,
                updatedAt = tenant.updatedAt,
                createdByName = tenant.createdBy?.let { "${it.firstName} ${it.lastName}" }
            )
        }
    }
}

/**
 * Minimal tenant response for lists and search results.
 * Reduces payload size for large result sets.
 */
data class TenantBasicResponse(
    val id: UUID,
    val tenantId: String,
    val name: String,
    val status: TenantStatus,
    val subscriptionStatus: SubscriptionStatus,
    val planTier: PlanTier,
    val contactEmail: String,
    val facilityType: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(tenant: Tenant): TenantBasicResponse {
            return TenantBasicResponse(
                id = tenant.id!!,
                tenantId = tenant.tenantId,
                name = tenant.name,
                status = tenant.status,
                subscriptionStatus = tenant.subscriptionStatus,
                planTier = tenant.planTier,
                contactEmail = tenant.contactEmail,
                facilityType = tenant.facilityType,
                createdAt = tenant.createdAt!!
            )
        }
    }
}
