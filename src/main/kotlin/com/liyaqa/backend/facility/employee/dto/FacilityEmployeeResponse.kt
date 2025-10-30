package com.liyaqa.backend.facility.employee.dto

import com.liyaqa.backend.facility.employee.domain.FacilityEmployee
import com.liyaqa.backend.facility.employee.domain.FacilityEmployeeStatus
import com.liyaqa.backend.facility.employee.domain.FacilityPermission
import java.time.Instant
import java.util.*

/**
 * Response DTO for facility employee with full details.
 */
data class FacilityEmployeeResponse(
    val id: UUID,
    val tenantId: String,
    val facilityId: UUID,
    val facilityName: String,

    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String,

    val employeeNumber: String?,
    val jobTitle: String?,
    val department: String?,
    val hireDate: Instant?,

    val phoneNumber: String?,
    val emergencyContactName: String?,
    val emergencyContactPhone: String?,

    val status: FacilityEmployeeStatus,
    val suspendedAt: Instant?,
    val terminatedAt: Instant?,

    val lastLoginAt: Instant?,
    val lastLoginIp: String?,

    val groups: List<FacilityEmployeeGroupBasicResponse>,
    val permissions: Set<FacilityPermission>,

    val timezone: String,
    val locale: String,

    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long
) {
    companion object {
        fun from(employee: FacilityEmployee): FacilityEmployeeResponse {
            return FacilityEmployeeResponse(
                id = employee.id!!,
                tenantId = employee.tenantId,
                facilityId = employee.facility.id!!,
                facilityName = employee.facility.name,
                firstName = employee.firstName,
                lastName = employee.lastName,
                fullName = employee.getFullName(),
                email = employee.email,
                employeeNumber = employee.employeeNumber,
                jobTitle = employee.jobTitle,
                department = employee.department,
                hireDate = employee.hireDate,
                phoneNumber = employee.phoneNumber,
                emergencyContactName = employee.emergencyContactName,
                emergencyContactPhone = employee.emergencyContactPhone,
                status = employee.status,
                suspendedAt = employee.suspendedAt,
                terminatedAt = employee.terminatedAt,
                lastLoginAt = employee.lastLoginAt,
                lastLoginIp = employee.lastLoginIp,
                groups = employee.groups.map { FacilityEmployeeGroupBasicResponse.from(it) },
                permissions = employee.getAllPermissions(),
                timezone = employee.timezone,
                locale = employee.locale,
                createdAt = employee.createdAt,
                updatedAt = employee.updatedAt,
                version = employee.version
            )
        }
    }
}

/**
 * Response DTO for facility employee with basic information (for lists).
 */
data class FacilityEmployeeBasicResponse(
    val id: UUID,
    val fullName: String,
    val email: String,
    val jobTitle: String?,
    val status: FacilityEmployeeStatus,
    val lastLoginAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(employee: FacilityEmployee): FacilityEmployeeBasicResponse {
            return FacilityEmployeeBasicResponse(
                id = employee.id!!,
                fullName = employee.getFullName(),
                email = employee.email,
                jobTitle = employee.jobTitle,
                status = employee.status,
                lastLoginAt = employee.lastLoginAt,
                createdAt = employee.createdAt
            )
        }
    }
}
