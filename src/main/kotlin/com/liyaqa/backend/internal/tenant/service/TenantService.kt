package com.liyaqa.backend.internal.tenant.service

import com.liyaqa.backend.internal.audit.domain.AuditAction
import com.liyaqa.backend.internal.audit.domain.EntityType
import com.liyaqa.backend.internal.employee.domain.Employee
import com.liyaqa.backend.internal.employee.domain.Permission
import com.liyaqa.backend.internal.tenant.domain.*
import com.liyaqa.backend.internal.tenant.dto.*
import com.liyaqa.backend.internal.tenant.data.TenantRepository
import com.liyaqa.backend.internal.audit.service.AuditService
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Service for managing tenant lifecycle and operations.
 *
 * This is the central component for customer organization management.
 * Handles everything from onboarding to suspension to termination,
 * with comprehensive audit logging for compliance.
 *
 * Business Rules:
 * - Tenant IDs and subdomains must be globally unique
 * - Only TENANT_CREATE permission can create tenants
 * - Only TENANT_SUSPEND permission can suspend/reactivate
 * - Suspension requires documented reason
 * - Termination is soft delete (data retained)
 * - All actions are audit logged for compliance
 */
@Service
@Transactional
class TenantService(
    private val tenantRepository: TenantRepository,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new tenant organization.
     *
     * This initiates the customer onboarding process. The tenant starts
     * in PENDING_ACTIVATION status and must be explicitly activated.
     *
     * @throws IllegalArgumentException if tenant ID or subdomain already exists
     * @throws SecurityException if employee lacks TENANT_CREATE permission
     */
    fun createTenant(request: TenantCreateRequest, createdBy: Employee): TenantResponse {
        // Permission check
        if (!createdBy.hasPermission(Permission.TENANT_CREATE)) {
            logger.warn("Employee ${createdBy.id} attempted to create tenant without permission")
            auditService.logUnauthorizedAccess(
                createdBy,
                "Attempted to create tenant",
                EntityType.TENANT
            )
            throw SecurityException("Insufficient permissions to create tenant")
        }

        // Validation: tenant ID uniqueness
        if (tenantRepository.existsByTenantId(request.tenantId)) {
            throw IllegalArgumentException("Tenant ID '${request.tenantId}' already exists")
        }

        // Validation: subdomain uniqueness
        request.subdomain?.let { subdomain ->
            if (tenantRepository.existsBySubdomain(subdomain)) {
                throw IllegalArgumentException("Subdomain '$subdomain' already exists")
            }
        }

        // Create tenant entity
        val tenant = Tenant(
            tenantId = request.tenantId,
            name = request.name,
            contactEmail = request.contactEmail,
            contactPhone = request.contactPhone,
            contactPerson = request.contactPerson,
            billingEmail = request.billingEmail,
            billingAddress = request.billingAddress,
            taxId = request.taxId,
            planTier = request.planTier,
            subscriptionStatus = SubscriptionStatus.TRIAL, // Start with trial
            subdomain = request.subdomain,
            contractStartDate = request.contractStartDate ?: LocalDate.now(),
            contractEndDate = request.contractEndDate,
            description = request.description,
            facilityType = request.facilityType,
            timezone = request.timezone,
            locale = request.locale,
            status = TenantStatus.PENDING_ACTIVATION,
            createdBy = createdBy
        )

        val savedTenant = tenantRepository.save(tenant)

        // Audit log
        auditService.logCreate(
            createdBy,
            EntityType.TENANT,
            savedTenant.id!!,
            mapOf(
                "tenant_id" to savedTenant.tenantId,
                "name" to savedTenant.name,
                "plan_tier" to savedTenant.planTier.name,
                "facility_type" to (savedTenant.facilityType ?: "N/A")
            )
        )

        logger.info("Tenant created: ${savedTenant.tenantId} by employee ${createdBy.id}")

        return TenantResponse.from(savedTenant)
    }

    /**
     * Get tenant by ID.
     *
     * @throws EntityNotFoundException if tenant not found
     */
    @Transactional(readOnly = true)
    fun getTenantById(id: UUID, requestedBy: Employee): TenantResponse {
        checkPermission(requestedBy, Permission.TENANT_VIEW)

        val tenant = tenantRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tenant not found: $id") }

        return TenantResponse.from(tenant)
    }

    /**
     * Get tenant by tenant ID (string identifier).
     */
    @Transactional(readOnly = true)
    fun getTenantByTenantId(tenantId: String, requestedBy: Employee): TenantResponse {
        checkPermission(requestedBy, Permission.TENANT_VIEW)

        val tenant = tenantRepository.findByTenantId(tenantId)
            ?: throw EntityNotFoundException("Tenant not found: $tenantId")

        return TenantResponse.from(tenant)
    }

    /**
     * Update tenant information.
     *
     * Only provided fields are updated (partial update).
     */
    fun updateTenant(id: UUID, request: TenantUpdateRequest, updatedBy: Employee): TenantResponse {
        checkPermission(updatedBy, Permission.TENANT_UPDATE)

        val tenant = tenantRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tenant not found: $id") }

        val changes = mutableMapOf<String, String>()

        request.name?.let {
            if (it != tenant.name) {
                changes["name"] = "${tenant.name} -> $it"
                tenant.name = it
            }
        }

        request.contactEmail?.let {
            if (it != tenant.contactEmail) {
                changes["contact_email"] = "${tenant.contactEmail} -> $it"
                tenant.contactEmail = it
            }
        }

        request.contactPhone?.let {
            if (it != tenant.contactPhone) {
                changes["contact_phone"] = "${tenant.contactPhone ?: "null"} -> $it"
                tenant.contactPhone = it
            }
        }

        request.contactPerson?.let {
            if (it != tenant.contactPerson) {
                changes["contact_person"] = "${tenant.contactPerson ?: "null"} -> $it"
                tenant.contactPerson = it
            }
        }

        request.billingEmail?.let {
            if (it != tenant.billingEmail) {
                changes["billing_email"] = "${tenant.billingEmail} -> $it"
                tenant.billingEmail = it
            }
        }

        request.billingAddress?.let {
            if (it != tenant.billingAddress) {
                changes["billing_address"] = "updated"
                tenant.billingAddress = it
            }
        }

        request.taxId?.let {
            if (it != tenant.taxId) {
                changes["tax_id"] = "updated"
                tenant.taxId = it
            }
        }

        request.planTier?.let {
            if (it != tenant.planTier) {
                changes["plan_tier"] = "${tenant.planTier.name} -> ${it.name}"
                tenant.planTier = it
            }
        }

        request.contractStartDate?.let {
            if (it != tenant.contractStartDate) {
                changes["contract_start_date"] = "${tenant.contractStartDate} -> $it"
                tenant.contractStartDate = it
            }
        }

        request.contractEndDate?.let {
            if (it != tenant.contractEndDate) {
                changes["contract_end_date"] = "${tenant.contractEndDate ?: "null"} -> $it"
                tenant.contractEndDate = it
            }
        }

        request.description?.let { tenant.description = it }
        request.facilityType?.let { tenant.facilityType = it }
        request.timezone?.let { tenant.timezone = it }
        request.locale?.let { tenant.locale = it }

        val savedTenant = tenantRepository.save(tenant)

        // Audit log only if changes were made
        if (changes.isNotEmpty()) {
            auditService.logUpdate(
                updatedBy,
                EntityType.TENANT,
                savedTenant.id!!,
                changes
            )
        }

        logger.info("Tenant updated: ${tenant.tenantId} by employee ${updatedBy.id}")

        return TenantResponse.from(savedTenant)
    }

    /**
     * Suspend tenant (temporary block).
     *
     * Suspended tenants cannot access the platform but data is retained.
     * Common reasons: payment failure, terms violation, security concern.
     */
    fun suspendTenant(id: UUID, request: SuspendTenantRequest, suspendedBy: Employee): TenantResponse {
        checkPermission(suspendedBy, Permission.TENANT_SUSPEND)

        val tenant = tenantRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tenant not found: $id") }

        if (tenant.status == TenantStatus.SUSPENDED) {
            throw IllegalStateException("Tenant is already suspended")
        }

        if (tenant.status == TenantStatus.TERMINATED) {
            throw IllegalStateException("Cannot suspend terminated tenant")
        }

        tenant.suspend(request.reason, suspendedBy)
        val savedTenant = tenantRepository.save(tenant)

        auditService.logSecurityEvent(
            suspendedBy,
            AuditAction.TENANT_SUSPENDED,
            EntityType.TENANT,
            savedTenant.id!!,
            mapOf(
                "tenant_id" to tenant.tenantId,
                "reason" to request.reason
            )
        )

        logger.warn("Tenant suspended: ${tenant.tenantId} by ${suspendedBy.email}. Reason: ${request.reason}")

        return TenantResponse.from(savedTenant)
    }

    /**
     * Reactivate suspended tenant.
     */
    fun reactivateTenant(id: UUID, reactivatedBy: Employee): TenantResponse {
        checkPermission(reactivatedBy, Permission.TENANT_SUSPEND)

        val tenant = tenantRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tenant not found: $id") }

        if (tenant.status != TenantStatus.SUSPENDED) {
            throw IllegalStateException("Only suspended tenants can be reactivated")
        }

        val previousReason = tenant.suspensionReason

        tenant.reactivate()
        val savedTenant = tenantRepository.save(tenant)

        auditService.logSecurityEvent(
            reactivatedBy,
            AuditAction.TENANT_CREATED, // TODO: Add TENANT_REACTIVATED action
            EntityType.TENANT,
            savedTenant.id!!,
            mapOf(
                "tenant_id" to tenant.tenantId,
                "previous_suspension_reason" to (previousReason ?: "N/A")
            )
        )

        logger.info("Tenant reactivated: ${tenant.tenantId} by ${reactivatedBy.email}")

        return TenantResponse.from(savedTenant)
    }

    /**
     * Terminate tenant (permanent closure).
     *
     * This is a soft delete - data is retained for compliance/recovery.
     * Access is permanently blocked.
     */
    fun terminateTenant(id: UUID, terminatedBy: Employee): TenantResponse {
        checkPermission(terminatedBy, Permission.TENANT_DELETE)

        val tenant = tenantRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tenant not found: $id") }

        if (tenant.status == TenantStatus.TERMINATED) {
            throw IllegalStateException("Tenant is already terminated")
        }

        tenant.terminate()
        val savedTenant = tenantRepository.save(tenant)

        auditService.logDelete(
            terminatedBy,
            EntityType.TENANT,
            savedTenant.id!!,
            mapOf(
                "tenant_id" to tenant.tenantId,
                "name" to tenant.name,
                "final_plan_tier" to tenant.planTier.name
            )
        )

        logger.warn("Tenant terminated: ${tenant.tenantId} by ${terminatedBy.email}")

        return TenantResponse.from(savedTenant)
    }

    /**
     * Accept terms and conditions for tenant.
     */
    fun acceptTerms(id: UUID, request: AcceptTermsRequest, acceptedBy: Employee): TenantResponse {
        checkPermission(acceptedBy, Permission.TENANT_UPDATE)

        val tenant = tenantRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tenant not found: $id") }

        tenant.acceptTerms(request.acceptedBy, request.termsVersion)
        val savedTenant = tenantRepository.save(tenant)

        auditService.logUpdate(
            acceptedBy,
            EntityType.TENANT,
            savedTenant.id!!,
            mapOf(
                "action" to "terms_accepted",
                "terms_version" to request.termsVersion,
                "accepted_by_name" to request.acceptedBy
            )
        )

        logger.info("Terms accepted for tenant: ${tenant.tenantId}, version: ${request.termsVersion}")

        return TenantResponse.from(savedTenant)
    }

    /**
     * Change subscription plan for tenant.
     */
    fun changePlan(id: UUID, request: ChangePlanRequest, changedBy: Employee): TenantResponse {
        checkPermission(changedBy, Permission.TENANT_UPDATE)

        val tenant = tenantRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tenant not found: $id") }

        val oldPlan = tenant.planTier
        val newPlan = request.newPlanTier

        if (oldPlan == newPlan) {
            throw IllegalArgumentException("Tenant is already on ${newPlan.name} plan")
        }

        val isUpgrade = newPlan.ordinal > oldPlan.ordinal

        if (isUpgrade) {
            tenant.upgradePlan(newPlan)
        } else {
            tenant.downgradePlan(newPlan)
        }

        val savedTenant = tenantRepository.save(tenant)

        auditService.logUpdate(
            changedBy,
            EntityType.TENANT,
            savedTenant.id!!,
            mapOf(
                "action" to if (isUpgrade) "plan_upgrade" else "plan_downgrade",
                "old_plan" to oldPlan.name,
                "new_plan" to newPlan.name
            )
        )

        logger.info("Plan changed for tenant ${tenant.tenantId}: ${oldPlan.name} -> ${newPlan.name}")

        return TenantResponse.from(savedTenant)
    }

    /**
     * Search and filter tenants.
     */
    @Transactional(readOnly = true)
    fun searchTenants(filter: TenantSearchFilter, pageable: Pageable, requestedBy: Employee): Page<TenantBasicResponse> {
        checkPermission(requestedBy, Permission.TENANT_VIEW)

        val page = tenantRepository.searchTenants(
            filter.searchTerm,
            filter.status,
            filter.subscriptionStatus,
            filter.planTier,
            filter.facilityType,
            filter.includeSuspended,
            filter.includeTerminated,
            pageable
        )

        // Log if sensitive search (includes terminated)
        if (filter.includeTerminated) {
            auditService.logSensitiveSearch(
                requestedBy,
                "Searched tenants including terminated",
                EntityType.TENANT
            )
        }

        return page.map { TenantBasicResponse.from(it) }
    }

    /**
     * Get tenants requiring attention (past due, expiring contracts, etc.).
     */
    @Transactional(readOnly = true)
    fun getTenantsNeedingAttention(requestedBy: Employee): Map<String, List<TenantBasicResponse>> {
        checkPermission(requestedBy, Permission.TENANT_VIEW)

        val today = LocalDate.now()
        val in30Days = today.plusDays(30)

        return mapOf(
            "past_due" to tenantRepository.findPastDueTenants().map { TenantBasicResponse.from(it) },
            "suspended" to tenantRepository.findSuspendedTenants().take(10).map { TenantBasicResponse.from(it) },
            "expiring_contracts" to tenantRepository.findExpiringContracts(today, in30Days)
                .map { TenantBasicResponse.from(it) },
            "expired_contracts" to tenantRepository.findExpiredContracts(today).take(10)
                .map { TenantBasicResponse.from(it) }
        )
    }

    /**
     * Get tenant analytics and statistics.
     */
    @Transactional(readOnly = true)
    fun getTenantAnalytics(requestedBy: Employee): Map<String, Any> {
        checkPermission(requestedBy, Permission.TENANT_VIEW)

        val totalTenants = tenantRepository.count()
        val activeTenants = tenantRepository.countActiveTenants()
        val suspendedTenants = tenantRepository.countByStatus(TenantStatus.SUSPENDED)
        val terminatedTenants = tenantRepository.countByStatus(TenantStatus.TERMINATED)

        val byPlanTier = tenantRepository.countActiveByPlanTier()
            .associate { (it[0] as PlanTier).name to it[1] as Long }

        val bySubscriptionStatus = tenantRepository.countBySubscriptionStatusGrouped()
            .associate { (it[0] as SubscriptionStatus).name to it[1] as Long }

        return mapOf(
            "total_tenants" to totalTenants,
            "active_tenants" to activeTenants,
            "suspended_tenants" to suspendedTenants,
            "terminated_tenants" to terminatedTenants,
            "by_plan_tier" to byPlanTier,
            "by_subscription_status" to bySubscriptionStatus
        )
    }

    /**
     * Check if employee has required permission.
     */
    private fun checkPermission(employee: Employee, permission: Permission) {
        if (!employee.hasPermission(permission)) {
            logger.warn("Employee ${employee.id} lacks permission: ${permission.name}")
            auditService.logUnauthorizedAccess(
                employee,
                "Attempted action requiring ${permission.name}",
                EntityType.TENANT
            )
            throw SecurityException("Insufficient permissions: ${permission.name} required")
        }
    }
}
