package com.liyaqa.backend.internal.dto.tenant

import com.liyaqa.backend.internal.domain.tenant.PlanTier
import com.liyaqa.backend.internal.domain.tenant.SubscriptionStatus
import com.liyaqa.backend.internal.domain.tenant.TenantStatus

/**
 * Search filter for querying tenants.
 *
 * Supports multiple filtering criteria to help internal team
 * quickly find tenants based on various dimensions.
 */
data class TenantSearchFilter(
    /**
     * Search by name, tenant ID, or contact email (partial match).
     */
    val searchTerm: String? = null,

    /**
     * Filter by operational status.
     */
    val status: TenantStatus? = null,

    /**
     * Filter by subscription/billing status.
     */
    val subscriptionStatus: SubscriptionStatus? = null,

    /**
     * Filter by plan tier.
     */
    val planTier: PlanTier? = null,

    /**
     * Filter by facility type (e.g., "Tennis Club", "Gym").
     */
    val facilityType: String? = null,

    /**
     * Include suspended tenants in results (default: false).
     */
    val includeSuspended: Boolean = false,

    /**
     * Include terminated tenants in results (default: false).
     */
    val includeTerminated: Boolean = false,

    /**
     * Filter by contract expiration (show only expired contracts).
     */
    val contractExpired: Boolean? = null,

    /**
     * Filter by terms acceptance status.
     */
    val termsAccepted: Boolean? = null
)
