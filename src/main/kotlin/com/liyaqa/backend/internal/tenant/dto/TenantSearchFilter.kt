package com.liyaqa.backend.internal.tenant.dto

import com.liyaqa.backend.internal.tenant.domain.PlanTier
import com.liyaqa.backend.internal.tenant.domain.SubscriptionStatus
import com.liyaqa.backend.internal.tenant.domain.TenantStatus

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
