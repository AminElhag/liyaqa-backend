package com.liyaqa.backend.internal.domain.audit

enum class AuditResult {
    SUCCESS,
    FAILURE,
    PARTIAL, // Some operations succeeded, some failed
    UNAUTHORIZED
}