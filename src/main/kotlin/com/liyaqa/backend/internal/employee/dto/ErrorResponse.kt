package com.liyaqa.backend.internal.employee.dto

import java.time.Instant

data class ErrorResponse(
    val error: String,
    val message: String,
    val details: Map<String, Any>? = null,
    val timestamp: Instant = Instant.now()
)