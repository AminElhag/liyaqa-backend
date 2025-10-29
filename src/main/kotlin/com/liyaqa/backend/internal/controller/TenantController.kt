package com.liyaqa.backend.internal.controller

import com.liyaqa.backend.internal.domain.employee.Employee
import com.liyaqa.backend.internal.dto.tenant.*
import com.liyaqa.backend.internal.service.TenantService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST controller for tenant (customer organization) management.
 *
 * This API allows the internal team to manage sports facility organizations
 * throughout their lifecycle: onboarding, updates, plan changes, suspension,
 * and termination.
 *
 * Base URL: /api/v1/internal/tenants
 *
 * Security: All endpoints require authentication. Specific permissions
 * are enforced by the service layer.
 */
@RestController
@RequestMapping("/api/v1/internal/tenants")
class TenantController(
    private val tenantService: TenantService
) {

    /**
     * Create a new tenant organization.
     *
     * POST /api/v1/internal/tenants
     *
     * Requires: TENANT_CREATE permission
     *
     * Example Request:
     * {
     *   "tenantId": "acme-sports",
     *   "name": "Acme Sports Complex",
     *   "contactEmail": "contact@acmesports.com",
     *   "billingEmail": "billing@acmesports.com",
     *   "planTier": "PROFESSIONAL",
     *   "facilityType": "Multi-Sport Complex"
     * }
     */
    @PostMapping
    fun createTenant(
        @Valid @RequestBody request: TenantCreateRequest,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<TenantResponse> {
        val tenant = tenantService.createTenant(request, employee)
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant)
    }

    /**
     * Get tenant by ID.
     *
     * GET /api/v1/internal/tenants/{id}
     *
     * Requires: TENANT_VIEW permission
     */
    @GetMapping("/{id}")
    fun getTenantById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<TenantResponse> {
        val tenant = tenantService.getTenantById(id, employee)
        return ResponseEntity.ok(tenant)
    }

    /**
     * Get tenant by tenant ID (string identifier).
     *
     * GET /api/v1/internal/tenants/by-tenant-id/{tenantId}
     *
     * Requires: TENANT_VIEW permission
     */
    @GetMapping("/by-tenant-id/{tenantId}")
    fun getTenantByTenantId(
        @PathVariable tenantId: String,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<TenantResponse> {
        val tenant = tenantService.getTenantByTenantId(tenantId, employee)
        return ResponseEntity.ok(tenant)
    }

    /**
     * Update tenant information.
     *
     * PUT /api/v1/internal/tenants/{id}
     *
     * Requires: TENANT_EDIT permission
     *
     * Example Request:
     * {
     *   "name": "Acme Sports & Fitness",
     *   "contactPhone": "+1-555-0123",
     *   "billingAddress": "123 Main St, City, State 12345"
     * }
     */
    @PutMapping("/{id}")
    fun updateTenant(
        @PathVariable id: UUID,
        @Valid @RequestBody request: TenantUpdateRequest,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<TenantResponse> {
        val tenant = tenantService.updateTenant(id, request, employee)
        return ResponseEntity.ok(tenant)
    }

    /**
     * Search and filter tenants.
     *
     * GET /api/v1/internal/tenants?searchTerm=acme&status=ACTIVE&planTier=PROFESSIONAL
     *
     * Requires: TENANT_VIEW permission
     *
     * Query Parameters:
     * - searchTerm: Search by name, tenant ID, or email
     * - status: Filter by tenant status
     * - subscriptionStatus: Filter by subscription status
     * - planTier: Filter by plan tier
     * - facilityType: Filter by facility type
     * - includeSuspended: Include suspended tenants (default: false)
     * - includeTerminated: Include terminated tenants (default: false)
     * - page: Page number (0-indexed)
     * - size: Page size (default: 20)
     */
    @GetMapping
    fun searchTenants(
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) status: com.liyaqa.backend.internal.domain.tenant.TenantStatus?,
        @RequestParam(required = false) subscriptionStatus: com.liyaqa.backend.internal.domain.tenant.SubscriptionStatus?,
        @RequestParam(required = false) planTier: com.liyaqa.backend.internal.domain.tenant.PlanTier?,
        @RequestParam(required = false) facilityType: String?,
        @RequestParam(required = false, defaultValue = "false") includeSuspended: Boolean,
        @RequestParam(required = false, defaultValue = "false") includeTerminated: Boolean,
        @PageableDefault(size = 20) pageable: Pageable,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<Page<TenantBasicResponse>> {
        val filter = TenantSearchFilter(
            searchTerm = searchTerm,
            status = status,
            subscriptionStatus = subscriptionStatus,
            planTier = planTier,
            facilityType = facilityType,
            includeSuspended = includeSuspended,
            includeTerminated = includeTerminated
        )

        val tenants = tenantService.searchTenants(filter, pageable, employee)
        return ResponseEntity.ok(tenants)
    }

    /**
     * Suspend tenant (temporary block).
     *
     * POST /api/v1/internal/tenants/{id}/suspend
     *
     * Requires: TENANT_SUSPEND permission
     *
     * Example Request:
     * {
     *   "reason": "Payment overdue by 30 days"
     * }
     */
    @PostMapping("/{id}/suspend")
    fun suspendTenant(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SuspendTenantRequest,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<TenantResponse> {
        val tenant = tenantService.suspendTenant(id, request, employee)
        return ResponseEntity.ok(tenant)
    }

    /**
     * Reactivate suspended tenant.
     *
     * POST /api/v1/internal/tenants/{id}/reactivate
     *
     * Requires: TENANT_SUSPEND permission
     */
    @PostMapping("/{id}/reactivate")
    fun reactivateTenant(
        @PathVariable id: UUID,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<TenantResponse> {
        val tenant = tenantService.reactivateTenant(id, employee)
        return ResponseEntity.ok(tenant)
    }

    /**
     * Terminate tenant (permanent closure).
     *
     * DELETE /api/v1/internal/tenants/{id}
     *
     * Requires: TENANT_DELETE permission
     *
     * This is a soft delete - data is retained for compliance.
     */
    @DeleteMapping("/{id}")
    fun terminateTenant(
        @PathVariable id: UUID,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<TenantResponse> {
        val tenant = tenantService.terminateTenant(id, employee)
        return ResponseEntity.ok(tenant)
    }

    /**
     * Accept terms and conditions for tenant.
     *
     * POST /api/v1/internal/tenants/{id}/accept-terms
     *
     * Requires: TENANT_EDIT permission
     *
     * Example Request:
     * {
     *   "acceptedBy": "John Doe (Facility Manager)",
     *   "termsVersion": "2024-01"
     * }
     */
    @PostMapping("/{id}/accept-terms")
    fun acceptTerms(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AcceptTermsRequest,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<TenantResponse> {
        val tenant = tenantService.acceptTerms(id, request, employee)
        return ResponseEntity.ok(tenant)
    }

    /**
     * Change subscription plan for tenant.
     *
     * POST /api/v1/internal/tenants/{id}/change-plan
     *
     * Requires: TENANT_EDIT permission
     *
     * Example Request:
     * {
     *   "newPlanTier": "ENTERPRISE"
     * }
     */
    @PostMapping("/{id}/change-plan")
    fun changePlan(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangePlanRequest,
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<TenantResponse> {
        val tenant = tenantService.changePlan(id, request, employee)
        return ResponseEntity.ok(tenant)
    }

    /**
     * Get tenants requiring attention.
     *
     * GET /api/v1/internal/tenants/attention-needed
     *
     * Requires: TENANT_VIEW permission
     *
     * Returns categories of tenants that need internal team action:
     * - Past due payments
     * - Suspended accounts
     * - Expiring contracts (next 30 days)
     * - Expired contracts
     */
    @GetMapping("/attention-needed")
    fun getTenantsNeedingAttention(
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<Map<String, List<TenantBasicResponse>>> {
        val tenants = tenantService.getTenantsNeedingAttention(employee)
        return ResponseEntity.ok(tenants)
    }

    /**
     * Get tenant analytics and statistics.
     *
     * GET /api/v1/internal/tenants/analytics
     *
     * Requires: TENANT_VIEW permission
     *
     * Returns:
     * - Total tenant count
     * - Active/suspended/terminated breakdowns
     * - Distribution by plan tier
     * - Distribution by subscription status
     */
    @GetMapping("/analytics")
    fun getTenantAnalytics(
        @AuthenticationPrincipal employee: Employee
    ): ResponseEntity<Map<String, Any>> {
        val analytics = tenantService.getTenantAnalytics(employee)
        return ResponseEntity.ok(analytics)
    }
}
