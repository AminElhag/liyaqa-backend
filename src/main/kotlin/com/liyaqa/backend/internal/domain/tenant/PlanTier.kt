package com.liyaqa.backend.internal.domain.tenant

/**
 * Subscription plan tiers defining feature access and pricing.
 *
 * Each tier provides increasing levels of functionality and capacity
 * for sports facility management.
 */
enum class PlanTier {
    /**
     * Free tier - limited features for small facilities.
     * Typically includes:
     * - Basic scheduling (1 court/field)
     * - Up to 50 bookings/month
     * - Community support only
     */
    FREE,

    /**
     * Starter tier - entry-level paid plan for growing facilities.
     * Typically includes:
     * - Multi-court/field scheduling (up to 5)
     * - Up to 500 bookings/month
     * - Email support
     * - Basic reporting
     */
    STARTER,

    /**
     * Professional tier - comprehensive features for established facilities.
     * Typically includes:
     * - Unlimited courts/fields
     * - Unlimited bookings
     * - Priority email & phone support
     * - Advanced reporting & analytics
     * - Payment processing integration
     * - Member management
     * - Custom branding
     */
    PROFESSIONAL,

    /**
     * Enterprise tier - full platform access with dedicated support.
     * Typically includes:
     * - Everything in Professional
     * - Multi-location management
     * - Dedicated account manager
     * - SLA guarantees
     * - Custom integrations
     * - White-labeling options
     * - Advanced security features
     */
    ENTERPRISE,

    /**
     * Custom tier for special arrangements or legacy customers.
     * Features negotiated individually.
     */
    CUSTOM
}
