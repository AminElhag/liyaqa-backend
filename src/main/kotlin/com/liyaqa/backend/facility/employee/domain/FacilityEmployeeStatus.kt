package com.liyaqa.backend.facility.employee.domain

/**
 * Status of a facility employee.
 */
enum class FacilityEmployeeStatus {
    /**
     * Employee is active and can access the system.
     */
    ACTIVE,

    /**
     * Employee is suspended (temporary - can be reactivated).
     */
    SUSPENDED,

    /**
     * Employee account is inactive (not yet activated or deactivated).
     */
    INACTIVE,

    /**
     * Employee has been terminated (permanent - employment ended).
     */
    TERMINATED
}
