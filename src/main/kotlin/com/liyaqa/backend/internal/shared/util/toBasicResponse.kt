package com.liyaqa.backend.internal.shared.util

import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.employee.dto.EmployeeBasicResponse

fun Employee.toBasicResponse() = EmployeeBasicResponse(
    id = this.id!!,
    fullName = this.getFullName(),
    email = this.email,
    department = this.department,
    jobTitle = this.jobTitle,
    status = this.status
)