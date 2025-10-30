package com.liyaqa.backend.internal.employee.dto

import com.liyaqa.backend.internal.employee.domain.EmployeeStatus
import java.util.UUID

/**
 * Minimal employee representation for listings and references.
 * This reduces payload size by ~70% compared to full response,
 * critical for mobile performance.
 */
data class EmployeeBasicResponse(
    val id: UUID,
    val fullName: String,
    val email: String,
    val department: String,
    val jobTitle: String,
    val status: EmployeeStatus
)