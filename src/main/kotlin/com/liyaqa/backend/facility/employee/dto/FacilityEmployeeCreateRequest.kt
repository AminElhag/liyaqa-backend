package com.liyaqa.backend.facility.employee.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

/**
 * Request DTO for creating a new facility employee.
 */
data class FacilityEmployeeCreateRequest(
    @field:NotBlank(message = "Facility ID is required")
    val facilityId: UUID,

    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 255, message = "First name must be between 1 and 255 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 255, message = "Last name must be between 1 and 255 characters")
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

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

    val groupIds: List<UUID>? = null,

    val timezone: String = "UTC",
    val locale: String = "en_US"
)
