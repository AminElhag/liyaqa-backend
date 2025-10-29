package com.liyaqa.backend.internal.security

fun validatePasswordStrength(password: String) {
    require(password.length >= 12) { "Password must be at least 12 characters" }
    require(password.any { it.isDigit() }) { "Password must contain at least one digit" }
    require(password.any { it.isUpperCase() }) { "Password must contain at least one uppercase letter" }
    require(password.any { it.isLowerCase() }) { "Password must contain at least one lowercase letter" }
    require(password.any { !it.isLetterOrDigit() }) { "Password must contain at least one special character" }
}