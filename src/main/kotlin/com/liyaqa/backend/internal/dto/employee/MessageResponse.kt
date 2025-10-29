package com.liyaqa.backend.internal.dto.employee

import java.time.Instant

/**
 * Generic response wrappers for consistency across our API.
 * This design ensures frontend developers always know where
 * to find error details or success messages.
 */

data class MessageResponse(
    val message: String,
    val timestamp: Instant = Instant.now()
)