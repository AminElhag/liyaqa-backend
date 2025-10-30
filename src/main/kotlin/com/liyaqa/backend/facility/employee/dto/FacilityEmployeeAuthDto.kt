package com.liyaqa.backend.facility.employee.dto

import com.liyaqa.backend.facility.employee.domain.FacilityPermission
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * Request DTO for facility employee login.
 */
data class FacilityEmployeeLoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String,

    @field:NotBlank(message = "Facility ID is required")
    val facilityId: UUID
)

/**
 * Response DTO for successful login.
 */
data class FacilityEmployeeLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long, // seconds

    val employee: FacilityEmployeeBasicResponse,
    val permissions: Set<FacilityPermission>
)

/**
 * Request DTO for changing password.
 */
data class FacilityEmployeeChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String
)

/**
 * Request DTO for suspending an employee.
 */
data class SuspendFacilityEmployeeRequest(
    @field:NotBlank(message = "Reason is required")
    val reason: String
)

/**
 * Request DTO for terminating an employee.
 */
data class TerminateFacilityEmployeeRequest(
    @field:NotBlank(message = "Reason is required")
    val reason: String
)
