package com.liyaqa.backend.internal.dto.employee

import com.liyaqa.backend.internal.domain.employee.EmployeeStatus
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateEmployeeRequest(
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    val firstName: String? = null,

    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    val lastName: String? = null,

    val department: String? = null,
    val jobTitle: String? = null,

    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Invalid phone number format"
    )
    val phoneNumber: String? = null,

    val status: EmployeeStatus? = null
) {
    /**
     * Generates a human-readable summary of changes for audit logs.
     * This design choice improves audit trail readability without
     * requiring complex log parsing.
     */
    fun getChangeSummary(): String {
        val changes = mutableListOf<String>()
        firstName?.let { changes.add("first name") }
        lastName?.let { changes.add("last name") }
        department?.let { changes.add("department") }
        jobTitle?.let { changes.add("job title") }
        phoneNumber?.let { changes.add("phone number") }
        status?.let { changes.add("status to $it") }

        return if (changes.isEmpty()) {
            "No changes"
        } else {
            "Updated ${changes.joinToString(", ")}"
        }
    }
}