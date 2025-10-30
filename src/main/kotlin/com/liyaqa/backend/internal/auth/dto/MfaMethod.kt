package com.liyaqa.backend.internal.auth.dto

enum class MfaMethod {
    TOTP,      // Time-based one-time password (authenticator app)
    SMS,       // SMS code (less secure, being phased out)
    EMAIL,     // Email code (backup method)
    BACKUP_CODES, // Pre-generated backup codes
    WEBAUTHN   // Hardware security keys (most secure)
}