package com.liyaqa.backend.internal.auth.dto

import jakarta.validation.constraints.NotBlank

/**
 * Token refresh request for session extension.
 * 
 * Minimal structure as refresh tokens should be opaque to clients.
 * The validation ensures we're not processing empty or malformed tokens.
 */
data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)
