package com.liyaqa.backend.internal.domain.audit

enum class RiskLevel {
    LOW,      // Regular operations
    MEDIUM,   // Modifications to important data
    HIGH,     // Financial operations, tenant data access
    CRITICAL  // System configuration, mass operations, impersonation
}