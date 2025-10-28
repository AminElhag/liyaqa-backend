package com.liyaqa.backend.internal.security

import com.liyaqa.backend.internal.domain.employee.Permission

/**
 * Requires any of the specified permissions (OR logic).
 * 
 * This supports scenarios where multiple roles might have access
 * to the same functionality through different permission paths.
 * 
 * Usage:
 * ```
 * @RequireAnyPermission([Permission.EMPLOYEE_VIEW, Permission.EMPLOYEE_UPDATE])
 * fun getEmployeeDetails(...): Employee
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequireAnyPermission(
    val permissions: Array<Permission>
)