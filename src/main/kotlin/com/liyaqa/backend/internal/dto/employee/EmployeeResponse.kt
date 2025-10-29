package com.liyaqa.backend.internal.dto.employee

import com.liyaqa.backend.internal.domain.employee.EmployeeStatus
import com.liyaqa.backend.internal.domain.employee.Permission
import java.time.Instant
import java.util.UUID

/**
 * Response DTOs designed for different consumption contexts.
 * We provide multiple response types to avoid over-fetching and
 * reduce network overhead - a critical consideration for mobile admin apps.
 */

data class EmployeeResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String,
    val status: EmployeeStatus,
    val department: String,
    val jobTitle: String,
    val phoneNumber: String?,
    val groups: List<GroupResponse>,
    val permissions: Set<Permission>,
    val lastLoginAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)