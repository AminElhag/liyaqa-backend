package com.liyaqa.backend.internal.audit.domain

enum class RiskLevel {
    LOW,      // Regular operations
    MEDIUM,   // Modifications to important data
    HIGH,     // Financial operations, tenant data access
    CRITICAL  // System configuration, mass operations, impersonation
}