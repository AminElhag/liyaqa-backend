package com.liyaqa.backend.facility.employee.dto

import com.liyaqa.backend.facility.employee.domain.FacilityEmployeeStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

/**
 * Request DTO for updating a facility employee.
 * All fields are optional - only provided fields will be updated.
 */
data class FacilityEmployeeUpdateRequest(
    @field:Size(min = 1, max = 255, message = "First name must be between 1 and 255 characters")
    val firstName: String? = null,

    @field:Size(min = 1, max = 255, message = "Last name must be between 1 and 255 characters")
    val lastName: String? = null,

    @field:Email(message = "Invalid email format")
    val email: String? = null,

    @field:Size(max = 50, message = "Employee number must not exceed 50 characters")
    val employeeNumber: String? = null,

    @field:Size(max = 100, message = "Job title must not exceed 100 characters")
    val jobTitle: String? = null,

    @field:Size(max = 100, message = "Department must not exceed 100 characters")
    val department: String? = null,

    val hireDate: Instant? = null,

    @field:Size(max = 50, message = "Phone number must not exceed 50 characters")
    val phoneNumber: String? = null,

    val emergencyContactName: String? = null,

    @field:Size(max = 50, message = "Emergency contact phone must not exceed 50 characters")
    val emergencyContactPhone: String? = null,

    val status: FacilityEmployeeStatus? = null,

    val groupIds: List<UUID>? = null,

    val timezone: String? = null,
    val locale: String? = null
)
