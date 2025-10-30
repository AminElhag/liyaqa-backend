package com.liyaqa.backend.internal.employee.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.*

/**
 * DTOs serving as our API contract for employee management.
 *
 * These data structures embody our API design philosophy - they're not just
 * data carriers, but contracts that encode business rules through validation.
 * This design ensures invalid data never reaches our business layer, improving
 * system reliability and reducing defensive programming needs downstream.
 */

data class CreateEmployeeRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Department is required")
    val department: String,

    @field:NotBlank(message = "Job title is required")
    val jobTitle: String,

    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Invalid phone number format"
    )
    val phoneNumber: String? = null,

    val groupIds: Set<UUID>? = null
) {
    /**
     * Business validation beyond basic field constraints.
     * This allows us to encode complex rules that span multiple fields.
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // Ensure at least one group is assigned for new employees
        if (groupIds.isNullOrEmpty()) {
            errors.add("At least one group must be assigned to new employees")
        }

        // Validate email domain if we restrict to company emails
        if (!email.endsWith("@liyaqa.com") && !email.endsWith("@liyaqa.io")) {
            errors.add("Email must be a Liyaqa company email address")
        }

        return errors
    }
}









