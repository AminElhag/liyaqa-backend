package com.liyaqa.backend.internal.shared.security

import com.liyaqa.backend.internal.employee.domain.Permission
import org.springframework.security.access.prepost.PreAuthorize

/**
 * Requires specific permission(s) to access the annotated method.
 * 
 * This provides fine-grained access control at the method level,
 * supporting our principle of least privilege. Multiple permissions
 * can be required (AND logic) or any of several (OR logic).
 * 
 * Usage:
 * ```
 * @RequirePermission(Permission.EMPLOYEE_CREATE)
 * fun createEmployee(...): Employee
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('PERMISSION_' + #permission.name())")
annotation class RequirePermission(
    val permission: Permission
)