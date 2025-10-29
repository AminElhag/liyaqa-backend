package com.liyaqa.backend.internal.dto.employee

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 12, message = "Password must be at least 12 characters")
    @field:Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{12,}$",
        message = "Password must contain uppercase, lowercase, digit, and special character"
    )
    val newPassword: String,

    @field:NotBlank(message = "Password confirmation is required")
    val confirmPassword: String
) {
    /**
     * Custom validation ensuring password confirmation matches.
     * This prevents typos that could lock users out of their accounts.
     */
    @AssertTrue(message = "Passwords do not match")
    fun isPasswordsMatching(): Boolean = newPassword == confirmPassword
}