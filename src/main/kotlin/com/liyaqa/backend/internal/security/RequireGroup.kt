package com.liyaqa.backend.internal.security

import org.springframework.security.access.prepost.PreAuthorize

/**
 * Requires membership in specific group(s).
 * 
 * This provides role-based access control for scenarios where
 * permissions alone aren't sufficient. Groups represent organizational
 * roles rather than technical permissions.
 * 
 * Usage:
 * ```
 * @RequireGroup("Finance")
 * fun getFinancialReports(): List<Report>
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('GROUP_' + #group)")
annotation class RequireGroup(
    val group: String
)
