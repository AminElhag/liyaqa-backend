package com.liyaqa.backend.internal.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Login request with built-in validation rules.
 *
 * The validation here prevents common attack vectors:
 * - Email validation prevents injection attempts
 * - Size limits prevent DoS through oversized inputs
 * - NotBlank prevents null/empty confusion attacks
 */
data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email too long")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Invalid password length")
    val password: String,

    // Optional 2FA code for future enhancement
    val totpCode: String? = null,

    // Device fingerprint for risk assessment
    val deviceFingerprint: String? = null
) {
    /**
     * Sanitizes input to prevent log injection attacks.
     * This is defense in depth - we don't trust even validated input.
     */
    fun sanitizedEmail(): String = email.replace("\n", "").replace("\r", "")
}