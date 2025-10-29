package com.liyaqa.backend.internal.domain.tenant

/**
 * Subscription billing status for a tenant.
 *
 * This tracks the payment and subscription state independently from
 * the operational status. A tenant can be ACTIVE but have PAST_DUE
 * subscription, triggering different business rules.
 */
enum class SubscriptionStatus {
    /**
     * Free trial period - no payment required yet.
     * Typically 14-30 days for evaluation.
     */
    TRIAL,

    /**
     * Subscription is current and paid.
     * All features available based on plan tier.
     */
    ACTIVE,

    /**
     * Payment failed or overdue.
     * Grace period before suspension (typically 7-14 days).
     */
    PAST_DUE,

    /**
     * Subscription cancelled but still within paid period.
     * Access continues until contract_end_date.
     */
    CANCELLED,

    /**
     * Subscription expired or terminated.
     * No billing, access blocked.
     */
    EXPIRED,

    /**
     * Special status for lifetime deals or custom arrangements.
     */
    LIFETIME
}
