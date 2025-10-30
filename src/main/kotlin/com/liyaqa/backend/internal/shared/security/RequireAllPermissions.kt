package com.liyaqa.backend.internal.shared.security

import com.liyaqa.backend.internal.employee.domain.Permission

/**
 * Requires all of the specified permissions (AND logic).
 * 
 * This is for high-risk operations that require multiple permissions
 * as a safety check. It implements our defense-in-depth philosophy
 * at the authorization layer.
 * 
 * Usage:
 * ```
 * @RequireAllPermissions([Permission.PAYMENT_PROCESS, Permission.PAYMENT_APPROVE])
 * fun processRefund(...): Payment
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequireAllPermissions(
    val permissions: Array<Permission>
)