package com.liyaqa.backend.internal.dto.employee

data class PasswordResetResponse(
    val message: String,
    val temporaryPassword: String? = null,
    val mustChangeOnNextLogin: Boolean
)