package com.liyaqa.backend.internal.dto.employee

import java.time.Instant

data class ValidationErrorResponse(
    val error: String = "VALIDATION_ERROR",
    val message: String = "Validation failed",
    val fieldErrors: Map<String, List<String>>,
    val timestamp: Instant = Instant.now()
)