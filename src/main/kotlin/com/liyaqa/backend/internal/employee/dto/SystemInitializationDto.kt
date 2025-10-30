package com.liyaqa.backend.internal.employee.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request to initialize the system with an administrator account.
 */
data class SystemInitializationRequest(
    @field:NotBlank(message = "Administrator email is required")
    @field:Email(message = "Valid email address is required")
    val adminEmail: String,

    @field:NotBlank(message = "Administrator password is required")
    @field:Size(min = 12, message = "Password must be at least 12 characters for administrator account")
    val adminPassword: String,

    @field:NotBlank(message = "Administrator first name is required")
    val adminFirstName: String,

    @field:NotBlank(message = "Administrator last name is required")
    val adminLastName: String
)

/**
 * Response after system initialization.
 */
data class SystemInitializationResponse(
    val success: Boolean,
    val message: String,
    val administrator: EmployeeResponse,
    val groupsCreated: List<String>,
    val warningMessage: String = "IMPORTANT: Change the administrator password immediately!"
)

/**
 * System initialization status.
 */
data class InitializationStatusResponse(
    val isInitialized: Boolean,
    val employeeCount: Int,
    val groupCount: Int,
    val hasAdministrator: Boolean,
    val predefinedGroupsPresent: Boolean,
    val message: String
)
