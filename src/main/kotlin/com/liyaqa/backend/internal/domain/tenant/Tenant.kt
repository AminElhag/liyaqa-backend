package com.liyaqa.backend.internal.domain.tenant

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.domain.employee.Employee
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.time.LocalDate

/**
 * Represents a customer organization (sports facility) in the Liyaqa platform.
 *
 * Tenants are the paying customers who use Liyaqa to manage their sports facilities.
 * Each tenant represents a distinct organization with their own:
 * - Subscription plan and billing
 * - Contract terms and conditions
 * - Users and data (isolated via multi-tenancy)
 * - Configuration and branding
 *
 * Design Philosophy:
 * - Separation of tenant status (operational) vs subscription status (billing)
 * - Comprehensive audit trail (who created, who suspended, when)
 * - Contract lifecycle tracking (start, end, terms acceptance)
 * - Multi-tenancy support via subdomain
 * - Soft deletion (keep data for retention/compliance)
 */
@Entity
@Table(
    name = "tenants",
    indexes = [
        Index(name = "idx_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_tenant_status", columnList = "status"),
        Index(name = "idx_tenant_subscription_status", columnList = "subscription_status"),
        Index(name = "idx_tenant_plan_tier", columnList = "plan_tier"),
        Index(name = "idx_tenant_contact_email", columnList = "contact_email"),
        Index(name = "idx_tenant_subdomain", columnList = "subdomain")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Tenant(
    // Unique identifier for this tenant (e.g., "acme-sports", "downtown-gym")
    // Override BaseEntity's tenantId to use it as the actual tenant identifier
    @Column(name = "tenant_id", unique = true, nullable = false, length = 100)
    override var tenantId: String,

    // Organization name (e.g., "Acme Sports Complex", "Downtown Fitness Center")
    @Column(nullable = false)
    var name: String,

    // === Contact Information ===
    @Column(name = "contact_email", nullable = false)
    var contactEmail: String,

    @Column(name = "contact_phone", length = 50)
    var contactPhone: String? = null,

    @Column(name = "contact_person")
    var contactPerson: String? = null,

    // === Billing Information ===
    @Column(name = "billing_email", nullable = false)
    var billingEmail: String,

    @Column(name = "billing_address", columnDefinition = "TEXT")
    var billingAddress: String? = null,

    @Column(name = "tax_id", length = 100)
    var taxId: String? = null,

    // === Subscription & Plan ===
    @Column(name = "plan_tier", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var planTier: PlanTier = PlanTier.FREE,

    @Column(name = "subscription_status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var subscriptionStatus: SubscriptionStatus = SubscriptionStatus.TRIAL,

    // === Multi-tenancy Support ===
    // Subdomain for tenant-specific URL (e.g., "acme.liyaqa.com")
    @Column(unique = true, length = 100)
    var subdomain: String? = null,

    // === Contract & Legal ===
    @Column(name = "contract_start_date")
    var contractStartDate: LocalDate? = null,

    @Column(name = "contract_end_date")
    var contractEndDate: LocalDate? = null,

    @Column(name = "terms_accepted_at")
    var termsAcceptedAt: Instant? = null,

    @Column(name = "terms_accepted_by")
    var termsAcceptedBy: String? = null,

    @Column(name = "terms_version", length = 50)
    var termsVersion: String? = null,

    // === Additional Metadata ===
    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "facility_type", length = 100)
    var facilityType: String? = null, // e.g., "Tennis Club", "Multi-Sport Complex", "Gym"

    @Column(length = 50)
    var timezone: String = "UTC",

    @Column(length = 10)
    var locale: String = "en_US",

    // === Status Tracking ===
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: TenantStatus = TenantStatus.PENDING_ACTIVATION,

    @Column(name = "suspended_at")
    var suspendedAt: Instant? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspended_by_id")
    var suspendedBy: Employee? = null,

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    var suspensionReason: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null

) : BaseEntity() {

    /**
     * Check if tenant has active, paid subscription.
     */
    fun hasActiveSubscription(): Boolean {
        return subscriptionStatus == SubscriptionStatus.ACTIVE ||
               subscriptionStatus == SubscriptionStatus.TRIAL
    }

    /**
     * Check if tenant can access the platform.
     */
    fun canAccess(): Boolean {
        return status == TenantStatus.ACTIVE && hasActiveSubscription()
    }

    /**
     * Check if subscription is in grace period (past due but not expired).
     */
    fun isInGracePeriod(): Boolean {
        return subscriptionStatus == SubscriptionStatus.PAST_DUE
    }

    /**
     * Check if contract has expired.
     */
    fun isContractExpired(): Boolean {
        return contractEndDate?.isBefore(LocalDate.now()) == true
    }

    /**
     * Check if terms have been accepted.
     */
    fun hasAcceptedTerms(): Boolean {
        return termsAcceptedAt != null && termsVersion != null
    }

    /**
     * Suspend tenant with reason and actor.
     */
    fun suspend(reason: String, suspendedBy: Employee) {
        this.status = TenantStatus.SUSPENDED
        this.suspendedAt = Instant.now()
        this.suspendedBy = suspendedBy
        this.suspensionReason = reason
    }

    /**
     * Reactivate suspended tenant.
     */
    fun reactivate() {
        if (status == TenantStatus.SUSPENDED) {
            this.status = TenantStatus.ACTIVE
            this.suspendedAt = null
            this.suspendedBy = null
            this.suspensionReason = null
        }
    }

    /**
     * Terminate tenant (permanent).
     */
    fun terminate() {
        this.status = TenantStatus.TERMINATED
        this.subscriptionStatus = SubscriptionStatus.EXPIRED
    }

    /**
     * Accept terms and conditions.
     */
    fun acceptTerms(acceptedBy: String, version: String) {
        this.termsAcceptedAt = Instant.now()
        this.termsAcceptedBy = acceptedBy
        this.termsVersion = version
    }

    /**
     * Upgrade subscription plan.
     */
    fun upgradePlan(newTier: PlanTier) {
        if (newTier.ordinal > this.planTier.ordinal) {
            this.planTier = newTier
        }
    }

    /**
     * Downgrade subscription plan.
     */
    fun downgradePlan(newTier: PlanTier) {
        if (newTier.ordinal < this.planTier.ordinal) {
            this.planTier = newTier
        }
    }

    /**
     * Get display name for UI.
     */
    fun getDisplayName(): String {
        return name
    }

    /**
     * Get tenant URL if subdomain is configured.
     */
    fun getTenantUrl(baseDomain: String = "liyaqa.com"): String? {
        return subdomain?.let { "https://$it.$baseDomain" }
    }

    override fun toString(): String {
        return "Tenant(id=$id, tenantId='$tenantId', name='$name', status=$status, planTier=$planTier)"
    }
}
