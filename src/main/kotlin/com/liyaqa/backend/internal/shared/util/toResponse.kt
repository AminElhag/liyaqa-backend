package com.liyaqa.backend.internal.shared.util

import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.employee.dto.EmployeeResponse
import com.liyaqa.backend.internal.employee.dto.GroupResponse

fun Employee.toResponse() = EmployeeResponse(
    id = this.id!!,
    firstName = this.firstName,
    lastName = this.lastName,
    fullName = this.getFullName(),
    email = this.email,
    employeeNumber = this.employeeNumber,
    status = this.status,
    department = this.department,
    jobTitle = this.jobTitle,
    phoneNumber = this.phoneNumber,
    groups = this.groups.map { GroupResponse(it.id!!, it.name, it.permissions) },
    permissions = this.getAllPermissions(),
    lastLoginAt = this.lastLoginAt,
    failedLoginAttempts = this.failedLoginAttempts,
    mustChangePassword = this.mustChangePassword,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)