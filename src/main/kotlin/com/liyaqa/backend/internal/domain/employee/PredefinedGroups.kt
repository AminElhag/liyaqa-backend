package com.liyaqa.backend.internal.domain.employee

/**
 * Predefined groups for common roles in our organization.
 * These provide templates but can be customized per deployment.
 */
object PredefinedGroups {
    fun createSuperAdmin() = EmployeeGroup(
        name = "Super Admin",
        description = "Full system access - use sparingly",
        isSystem = true,
        permissions = Permission.values().toMutableSet()
    )
    
    fun createSupportAgent() = EmployeeGroup(
        name = "Support Agent",
        description = "Customer support team member",
        isSystem = true,
        permissions = mutableSetOf(
            Permission.TENANT_VIEW,
            Permission.SUPPORT_VIEW_TICKETS,
            Permission.SUPPORT_HANDLE_TICKETS,
            Permission.AUDIT_VIEW_LOGS
        )
    )
    
    fun createSupportManager() = EmployeeGroup(
        name = "Support Manager",
        description = "Support team leadership",
        isSystem = true,
        permissions = mutableSetOf(
            Permission.TENANT_VIEW,
            Permission.TENANT_ACCESS_DATA,
            Permission.SUPPORT_VIEW_TICKETS,
            Permission.SUPPORT_HANDLE_TICKETS,
            Permission.SUPPORT_ESCALATE,
            Permission.SUPPORT_IMPERSONATE_TENANT,
            Permission.EMPLOYEE_VIEW,
            Permission.AUDIT_VIEW_LOGS,
            Permission.AUDIT_EXPORT_REPORTS
        )
    )
    
    fun createSalesTeam() = EmployeeGroup(
        name = "Sales",
        description = "Sales and business development",
        isSystem = true,
        permissions = mutableSetOf(
            Permission.TENANT_VIEW,
            Permission.TENANT_CREATE,
            Permission.DEAL_VIEW,
            Permission.DEAL_CREATE,
            Permission.DEAL_UPDATE,
            Permission.PAYMENT_VIEW
        )
    )
    
    fun createFinanceTeam() = EmployeeGroup(
        name = "Finance",
        description = "Financial operations and accounting",
        isSystem = true,
        permissions = mutableSetOf(
            Permission.DEAL_VIEW,
            Permission.DEAL_APPROVE,
            Permission.PAYMENT_VIEW,
            Permission.PAYMENT_PROCESS,
            Permission.PAYMENT_REFUND,
            Permission.AUDIT_VIEW_LOGS,
            Permission.AUDIT_EXPORT_REPORTS
        )
    )
}