package com.liyaqa.backend.internal.service

import com.liyaqa.backend.internal.domain.employee.Employee
import com.liyaqa.backend.internal.dto.employee.EmployeeBasicResponse

fun Employee.toBasicResponse() = EmployeeBasicResponse(
    id = this.id!!,
    fullName = this.getFullName(),
    email = this.email,
    department = this.department,
    jobTitle = this.jobTitle,
    status = this.status
)