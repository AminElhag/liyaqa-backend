package com.liyaqa.backend.internal.domain.audit

import java.util.*

/**
 * Builder for creating audit logs with fluent API.
 * This ensures we capture all necessary context consistently.
 */
class AuditLogBuilder {
    private var employeeId: UUID? = null
    private var employeeEmail: String? = null
    private var employeeName: String? = null
    private var action: AuditAction? = null
    private var entityType: EntityType? = null
    private var entityId: String? = null
    private var description: String? = null
    private var oldValue: String? = null
    private var newValue: String? = null
    private var ipAddress: String? = null
    private var userAgent: String? = null
    private var sessionId: String? = null
    private var affectedTenantId: String? = null
    private var riskLevel: RiskLevel = RiskLevel.LOW
    private var result: AuditResult = AuditResult.SUCCESS
    private var errorMessage: String? = null
    private var durationMs: Long? = null

    fun employee(id: UUID, email: String, name: String) = apply {
        this.employeeId = id
        this.employeeEmail = email
        this.employeeName = name
    }

    fun action(action: AuditAction) = apply { this.action = action }
    fun entity(type: EntityType, id: String? = null) = apply {
        this.entityType = type
        this.entityId = id
    }

    fun description(desc: String) = apply { this.description = desc }
    fun changes(old: String?, new: String?) = apply {
        this.oldValue = old
        this.newValue = new
    }

    fun request(ip: String?, userAgent: String?, sessionId: String?) = apply {
        this.ipAddress = ip
        this.userAgent = userAgent
        this.sessionId = sessionId
    }

    fun affectsTenant(tenantId: String?) = apply { this.affectedTenantId = tenantId }
    fun withRisk(level: RiskLevel) = apply { this.riskLevel = level }
    fun withResult(result: AuditResult, error: String? = null) = apply {
        this.result = result
        this.errorMessage = error
    }

    fun duration(ms: Long) = apply { this.durationMs = ms }



    fun build(): AuditLog {
        require(employeeId != null) { "Employee ID is required" }
        require(action != null) { "Action is required" }
        require(entityType != null) { "Entity type is required" }
        require(description != null) { "Description is required" }

        return AuditLog(
            employeeId = employeeId!!,
            employeeEmail = employeeEmail!!,
            employeeName = employeeName!!,
            action = action!!,
            entityType = entityType!!,
            entityId = entityId,
            description = description!!,
            oldValue = oldValue,
            newValue = newValue,
            ipAddress = ipAddress,
            userAgent = userAgent,
            sessionId = sessionId,
            affectedTenantId = affectedTenantId,
            riskLevel = riskLevel,
            result = result,
            errorMessage = errorMessage,
            durationMs = durationMs
        )
    }
}