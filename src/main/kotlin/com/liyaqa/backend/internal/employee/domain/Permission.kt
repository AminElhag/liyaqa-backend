package com.liyaqa.backend.internal.employee.domain

/**
 * Fine-grained permissions following the principle of least privilege.
 * Each permission maps to specific actions in our control plane.
 */
enum class Permission {
    // Employee Management
    EMPLOYEE_VIEW,
    EMPLOYEE_CREATE,
    EMPLOYEE_UPDATE,
    EMPLOYEE_DELETE,
    EMPLOYEE_SUSPEND,
    
    // Group Management
    GROUP_VIEW,
    GROUP_CREATE,
    GROUP_UPDATE,
    GROUP_DELETE,
    GROUP_ASSIGN_PERMISSIONS,
    
    // Tenant Management
    TENANT_VIEW,
    TENANT_CREATE,
    TENANT_UPDATE,
    TENANT_DELETE,
    TENANT_SUSPEND,
    TENANT_ACCESS_DATA, // Can access tenant's data for support

    // Facility Management
    FACILITY_VIEW,
    FACILITY_CREATE,
    FACILITY_UPDATE,
    FACILITY_DELETE,
    FACILITY_MANAGE_BRANCHES, // Can add/edit/delete branches

    // Support Operations
    SUPPORT_VIEW_TICKETS,
    SUPPORT_HANDLE_TICKETS,
    SUPPORT_ESCALATE,
    SUPPORT_IMPERSONATE_TENANT, // Dangerous - for senior support only
    
    // Deal & Payment Management
    DEAL_VIEW,
    DEAL_CREATE,
    DEAL_UPDATE,
    DEAL_APPROVE,
    PAYMENT_VIEW,
    PAYMENT_PROCESS,
    PAYMENT_REFUND,
    
    // System Administration
    SYSTEM_VIEW_LOGS,
    SYSTEM_VIEW_METRICS,
    SYSTEM_CONFIGURE,
    SYSTEM_MAINTENANCE_MODE,
    
    // Audit & Compliance
    AUDIT_VIEW_LOGS,
    AUDIT_EXPORT_REPORTS,
    AUDIT_DELETE_LOGS, // Very restricted - compliance only

    // Booking Management (Facility Operations)
    BOOKING_VIEW,
    BOOKING_CREATE,
    BOOKING_UPDATE,
    BOOKING_CANCEL,
    BOOKING_CHECKIN,
    BOOKING_MANAGE_ALL, // Can manage any booking

    // Court Management
    COURT_VIEW,
    COURT_CREATE,
    COURT_UPDATE,
    COURT_DELETE,
    COURT_MANAGE,

    // Member Management (Facility)
    MEMBER_VIEW,
    MEMBER_CREATE,
    MEMBER_UPDATE,
    MEMBER_DELETE,

    // Membership Management
    MEMBERSHIP_VIEW,
    MEMBERSHIP_CREATE,
    MEMBERSHIP_CANCEL,
    MEMBERSHIP_MANAGE
}