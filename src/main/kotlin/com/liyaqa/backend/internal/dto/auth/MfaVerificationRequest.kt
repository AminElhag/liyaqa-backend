package com.liyaqa.backend.internal.dto.auth

import jakarta.validation.constraints.NotBlank

/**
 * MFA verification request.
 */
data class MfaVerificationRequest(
    @field:NotBlank(message = "Challenge ID is required")
    val challengeId: String,
    
    @field:NotBlank(message = "Verification code is required")
    val code: String,
    
    val method: MfaMethod
)