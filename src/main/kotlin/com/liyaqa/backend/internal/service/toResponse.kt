package com.liyaqa.backend.internal.service

import com.liyaqa.backend.internal.domain.employee.Employee
import com.liyaqa.backend.internal.dto.employee.EmployeeResponse
import com.liyaqa.backend.internal.dto.employee.GroupResponse

fun Employee.toResponse() = EmployeeResponse(
    id = this.id!!,
    firstName = this.firstName,
    lastName = this.lastName,
    fullName = this.getFullName(),
    email = this.email,
    status = this.status,
    department = this.department,
    jobTitle = this.jobTitle,
    phoneNumber = this.phoneNumber,
    groups = this.groups.map { GroupResponse(it.id!!, it.name, it.permissions) },
    permissions = this.getAllPermissions(),
    lastLoginAt = this.lastLoginAt,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)