package com.liyaqa.backend.internal.facility.domain

/**
 * Status of a facility branch (physical location).
 *
 * Lifecycle:
 * - ACTIVE: Branch is operational
 * - INACTIVE: Branch is temporarily closed
 * - UNDER_RENOVATION: Branch is being renovated
 * - TEMPORARILY_CLOSED: Short-term closure (weather, emergency)
 * - PERMANENTLY_CLOSED: Branch no longer operates
 */
enum class BranchStatus {
    /**
     * Branch is active and operational.
     */
    ACTIVE,

    /**
     * Branch is temporarily inactive.
     */
    INACTIVE,

    /**
     * Branch is undergoing renovation or construction.
     */
    UNDER_RENOVATION,

    /**
     * Branch is temporarily closed (weather, emergency, etc.).
     */
    TEMPORARILY_CLOSED,

    /**
     * Branch has been permanently closed.
     */
    PERMANENTLY_CLOSED
}
