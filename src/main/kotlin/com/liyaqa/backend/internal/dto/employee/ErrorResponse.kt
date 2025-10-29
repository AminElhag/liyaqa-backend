package com.liyaqa.backend.internal.dto.employee

import java.time.Instant

data class ErrorResponse(
    val error: String,
    val message: String,
    val details: Map<String, Any>? = null,
    val timestamp: Instant = Instant.now()
)