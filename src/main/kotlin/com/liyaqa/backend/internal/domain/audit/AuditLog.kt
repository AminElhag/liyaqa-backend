package com.liyaqa.backend.internal.domain.audit

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

/**
 * Immutable audit log capturing every action within our control plane.
 * This is our source of truth for compliance, debugging, and understanding
 * how our system evolves over time. We never delete these records.
 */
@Entity
@Table(
    name = "internal_audit_logs",
    indexes = [
        Index(name = "idx_audit_employee", columnList = "employee_id"),
        Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
        Index(name = "idx_audit_action", columnList = "action"),
        Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        Index(name = "idx_audit_tenant", columnList = "affected_tenant_id")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class AuditLog(
    // Who performed the action
    @Column(name = "employee_id", nullable = false)
    val employeeId: UUID,

    @Column(name = "employee_email", nullable = false)
    val employeeEmail: String,

    @Column(name = "employee_name", nullable = false)
    val employeeName: String,

    // What action was performed
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val action: AuditAction,

    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val entityType: EntityType,

    @Column(name = "entity_id")
    val entityId: String? = null,

    // Context and details
    @Column(columnDefinition = "TEXT")
    val description: String,

    @Column(name = "old_value", columnDefinition = "TEXT")
    val oldValue: String? = null, // JSON representation of previous state

    @Column(name = "new_value", columnDefinition = "TEXT")
    val newValue: String? = null, // JSON representation of new state

    // Request metadata
    @Column(name = "ip_address")
    val ipAddress: String? = null,

    @Column(name = "user_agent")
    val userAgent: String? = null,

    @Column(name = "session_id")
    val sessionId: String? = null,

    // Impact tracking
    @Column(name = "affected_tenant_id")
    val affectedTenantId: String? = null, // When action affects a specific tenant

    @Column(name = "risk_level", nullable = false)
    @Enumerated(EnumType.STRING)
    val riskLevel: RiskLevel = RiskLevel.LOW,

    // Outcome
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val result: AuditResult = AuditResult.SUCCESS,

    @Column(name = "error_message")
    val errorMessage: String? = null,

    // Performance metrics
    @Column(name = "duration_ms")
    val durationMs: Long? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null

    @CreatedDate
    @Column(nullable = false, updatable = false)
    val timestamp: Instant = Instant.now()
}

