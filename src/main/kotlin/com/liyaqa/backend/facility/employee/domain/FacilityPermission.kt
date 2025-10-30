package com.liyaqa.backend.facility.employee.domain

/**
 * Permissions for facility employees (staff working at sport facilities).
 *
 * These permissions control access to facility operations like bookings,
 * court management, customer management, etc.
 *
 * This is separate from internal (Liyaqa platform) permissions.
 */
enum class FacilityPermission {
    // Dashboard & Reporting
    DASHBOARD_VIEW,
    REPORTS_VIEW,
    REPORTS_EXPORT,

    // Booking Management
    BOOKING_VIEW,
    BOOKING_CREATE,
    BOOKING_UPDATE,
    BOOKING_CANCEL,
    BOOKING_CHECK_IN,

    // Court/Field Management
    COURT_VIEW,
    COURT_MANAGE,
    COURT_SCHEDULE,

    // Customer Management
    CUSTOMER_VIEW,
    CUSTOMER_CREATE,
    CUSTOMER_UPDATE,
    CUSTOMER_DELETE,

    // Membership Management
    MEMBERSHIP_VIEW,
    MEMBERSHIP_CREATE,
    MEMBERSHIP_UPDATE,
    MEMBERSHIP_CANCEL,

    // Payment Management
    PAYMENT_VIEW,
    PAYMENT_PROCESS,
    PAYMENT_REFUND,

    // Employee Management (for facility managers)
    EMPLOYEE_VIEW,
    EMPLOYEE_CREATE,
    EMPLOYEE_UPDATE,
    EMPLOYEE_DELETE,
    EMPLOYEE_MANAGE_PERMISSIONS,

    // Settings & Configuration
    SETTINGS_VIEW,
    SETTINGS_UPDATE,

    // Inventory Management (for pro shops, equipment)
    INVENTORY_VIEW,
    INVENTORY_MANAGE,

    // Event Management (tournaments, classes)
    EVENT_VIEW,
    EVENT_CREATE,
    EVENT_UPDATE,
    EVENT_DELETE
}
