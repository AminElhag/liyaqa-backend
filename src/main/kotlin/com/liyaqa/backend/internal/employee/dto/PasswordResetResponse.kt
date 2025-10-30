package com.liyaqa.backend.internal.employee.dto

data class PasswordResetResponse(
    val message: String,
    val temporaryPassword: String? = null,
    val mustChangeOnNextLogin: Boolean
)