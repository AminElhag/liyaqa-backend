package com.liyaqa.backend.internal.auth.dto

import java.time.Instant

/**
 * Login attempt tracking for security monitoring.
 * 
 * This DTO supports our threat detection system by providing
 * structured data about authentication attempts for analysis.
 */
data class LoginAttempt(
    val email: String,
    val success: Boolean,
    val ipAddress: String,
    val userAgent: String?,
    val timestamp: Instant,
    val failureReason: String? = null,
    val riskScore: Double = 0.0,
    val riskFactors: Set<RiskFactor> = emptySet()
)