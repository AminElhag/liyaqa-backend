package com.liyaqa.backend.internal.security

enum class RateLimitScope {
    USER,    // Per authenticated user
    IP,      // Per IP address
    GLOBAL,  // System-wide
    TENANT   // Per tenant (for multi-tenant operations)
}
