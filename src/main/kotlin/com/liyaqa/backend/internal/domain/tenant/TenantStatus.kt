package com.liyaqa.backend.internal.domain.tenant

/**
 * Tenant lifecycle status.
 *
 * This enum tracks the operational state of a sports facility organization
 * throughout its relationship with Liyaqa.
 */
enum class TenantStatus {
    /**
     * Normal operational state - tenant has full access to the platform.
     */
    ACTIVE,

    /**
     * Temporarily suspended - typically due to payment issues or terms violations.
     * Access is blocked but data is retained.
     */
    SUSPENDED,

    /**
     * Permanently closed - contract ended or terminated.
     * Access is blocked, data may be archived based on retention policy.
     */
    TERMINATED,

    /**
     * Onboarding in progress - account created but not fully configured.
     * Limited access while setup is completed.
     */
    PENDING_ACTIVATION
}
