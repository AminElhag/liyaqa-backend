package com.liyaqa.backend.internal.dto.auth

/**
 * Risk factors identified during authentication.
 * 
 * These flags help our security team understand why certain
 * login attempts were flagged or blocked.
 */
enum class RiskFactor {
    NEW_LOCATION,           // Login from previously unseen location
    NEW_DEVICE,            // Login from unrecognized device
    UNUSUAL_TIME,          // Login outside normal hours
    RAPID_ATTEMPTS,        // Multiple attempts in short period
    IMPOSSIBLE_TRAVEL,     // Geographic impossibility
    KNOWN_VPN,            // Using known VPN service
    SUSPICIOUS_PATTERN,    // Matches known attack patterns
    LEAKED_CREDENTIALS    // Password found in breach databases
}