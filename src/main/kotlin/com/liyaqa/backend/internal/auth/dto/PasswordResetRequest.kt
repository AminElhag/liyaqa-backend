package com.liyaqa.backend.internal.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Password reset initiation request.
 * 
 * Simple structure to prevent enumeration attacks - we only need
 * the email and will always return success regardless of existence.
 */
data class PasswordResetRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String
)