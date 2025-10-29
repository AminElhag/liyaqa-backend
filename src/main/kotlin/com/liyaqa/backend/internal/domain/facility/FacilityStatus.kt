package com.liyaqa.backend.internal.domain.facility

/**
 * Status of a sport facility.
 *
 * Lifecycle:
 * - ACTIVE: Facility is operational and accepting bookings
 * - INACTIVE: Facility is temporarily not accepting new bookings
 * - UNDER_MAINTENANCE: Facility is being renovated or maintained
 * - CLOSED: Facility is permanently closed
 */
enum class FacilityStatus {
    /**
     * Facility is active and operational.
     */
    ACTIVE,

    /**
     * Facility is temporarily inactive (seasonal closure, temporary suspension).
     */
    INACTIVE,

    /**
     * Facility is undergoing maintenance or renovation.
     */
    UNDER_MAINTENANCE,

    /**
     * Facility has been permanently closed.
     */
    CLOSED
}
