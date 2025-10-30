package com.liyaqa.backend.internal.auth.dto

import com.liyaqa.backend.internal.employee.domain.Permission
import com.liyaqa.backend.internal.employee.dto.EmployeeBasicResponse
import java.time.Instant

/**
 * Session validation response for proactive auth checks.
 * 
 * This allows clients to verify session validity before attempting
 * operations, improving user experience by avoiding mid-operation
 * auth failures.
 */
data class SessionValidationResponse(
    val valid: Boolean,
    val employee: EmployeeBasicResponse?,
    val permissions: Set<Permission>?,
    val sessionExpiresAt: Instant? = null,
    val requiresAction: Set<RequiredAction> = emptySet()
)