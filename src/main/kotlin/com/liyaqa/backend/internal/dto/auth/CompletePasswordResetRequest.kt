package com.liyaqa.backend.internal.dto.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Password reset completion request with new password.
 *
 * The token validation happens server-side. Password requirements
 * are enforced here to provide immediate feedback to users.
 */
data class CompletePasswordResetRequest(
    @field:NotBlank(message = "Reset token is required")
    val token: String,

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
     * Validates password confirmation matches.
     * This prevents typos that could lock users out.
     */
    fun validatePasswordMatch(): Boolean = newPassword == confirmPassword
}