package com.liyaqa.backend.internal.audit.domain

enum class AuditResult {
    SUCCESS,
    FAILURE,
    PARTIAL, // Some operations succeeded, some failed
    UNAUTHORIZED
}