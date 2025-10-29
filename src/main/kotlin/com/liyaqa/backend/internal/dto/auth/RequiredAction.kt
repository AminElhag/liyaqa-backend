package com.liyaqa.backend.internal.dto.auth

/**
 * Actions the user must complete before full access.
 * 
 * This enum supports our progressive security model where we can
 * require additional verification based on risk assessment.
 */
enum class RequiredAction {
    CHANGE_PASSWORD,      // Must change password before proceeding
    VERIFY_EMAIL,        // Email verification required
    SETUP_2FA,          // Two-factor authentication setup required
    ACCEPT_TERMS,       // New terms acceptance required
    COMPLETE_PROFILE    // Profile information missing
}