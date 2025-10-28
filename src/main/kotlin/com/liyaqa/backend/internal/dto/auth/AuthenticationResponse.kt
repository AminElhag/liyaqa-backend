package com.liyaqa.backend.internal.dto.auth

import com.liyaqa.backend.internal.dto.employee.EmployeeBasicResponse

/**
 * Authentication response containing tokens and user context.
 * 
 * This design provides everything the client needs to establish
 * a session while revealing minimal information about our internal
 * structure. The separation of access and refresh tokens supports
 * our security architecture with different lifetimes and purposes.
 */
data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Long, // Seconds until access token expires
    val requiresPasswordChange: Boolean = false,
    val requires2FA: Boolean = false,
    val employee: EmployeeBasicResponse?
)