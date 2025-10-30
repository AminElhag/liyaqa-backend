package com.liyaqa.backend.internal.employee.dto

import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.employee.domain.EmployeeStatus
import com.liyaqa.backend.internal.employee.domain.Permission
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
    val employeeNumber: String?,
    val status: EmployeeStatus,
    val department: String,
    val jobTitle: String,
    val phoneNumber: String?,
    val groups: List<GroupResponse>,
    val permissions: Set<Permission>,
    val lastLoginAt: Instant?,
    val failedLoginAttempts: Int,
    val mustChangePassword: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(employee: Employee): EmployeeResponse {
            return EmployeeResponse(
                id = employee.id!!,
                firstName = employee.firstName,
                lastName = employee.lastName,
                fullName = employee.getFullName(),
                email = employee.email,
                employeeNumber = employee.employeeNumber,
                status = employee.status,
                department = employee.department,
                jobTitle = employee.jobTitle,
                phoneNumber = employee.phoneNumber,
                groups = employee.groups.map { GroupResponse.from(it) },
                permissions = employee.getAllPermissions(),
                lastLoginAt = employee.lastLoginAt,
                failedLoginAttempts = employee.failedLoginAttempts,
                mustChangePassword = employee.mustChangePassword,
                createdAt = employee.createdAt,
                updatedAt = employee.updatedAt
            )
        }
    }
}