package com.liyaqa.backend.facility.employee.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.domain.facility.SportFacility
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant

/**
 * Employee who works at a sport facility.
 *
 * Examples: receptionist, coach, manager, cleaning staff, maintenance worker
 *
 * Key Features:
 * - Authentication with email/password
 * - Group-based permissions
 * - Scoped to specific facility (multi-tenancy)
 * - Status management (active, suspended, terminated)
 * - Contact information
 * - Employment tracking
 */
@Entity
@Table(
    name = "facility_employees",
    indexes = [
        Index(name = "idx_fac_emp_facility", columnList = "facility_id"),
        Index(name = "idx_fac_emp_email", columnList = "email"),
        Index(name = "idx_fac_emp_status", columnList = "status"),
        Index(name = "idx_fac_emp_employee_number", columnList = "employee_number")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_facility_employee_email", columnNames = ["facility_id", "email"]),
        UniqueConstraint(name = "uk_facility_employee_number", columnNames = ["facility_id", "employee_number"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class FacilityEmployee(
    // Facility this employee works at
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    // === Basic Information ===
    @Column(name = "first_name", nullable = false)
    var firstName: String,

    @Column(name = "last_name", nullable = false)
    var lastName: String,

    // === Authentication ===
    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    // === Employment Information ===
    @Column(name = "employee_number", length = 50)
    var employeeNumber: String? = null,

    @Column(name = "job_title", length = 100)
    var jobTitle: String? = null,

    @Column(name = "department", length = 100)
    var department: String? = null,

    @Column(name = "hire_date")
    var hireDate: Instant? = null,

    // === Contact Information ===
    @Column(name = "phone_number", length = 50)
    var phoneNumber: String? = null,

    @Column(name = "emergency_contact_name")
    var emergencyContactName: String? = null,

    @Column(name = "emergency_contact_phone", length = 50)
    var emergencyContactPhone: String? = null,

    // === Status ===
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: FacilityEmployeeStatus = FacilityEmployeeStatus.ACTIVE,

    @Column(name = "suspended_at")
    var suspendedAt: Instant? = null,

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    var suspensionReason: String? = null,

    @Column(name = "terminated_at")
    var terminatedAt: Instant? = null,

    @Column(name = "termination_reason", columnDefinition = "TEXT")
    var terminationReason: String? = null,

    // === Session Management ===
    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

    @Column(name = "last_login_ip", length = 45)
    var lastLoginIp: String? = null,

    @Column(name = "failed_login_attempts")
    var failedLoginAttempts: Int = 0,

    @Column(name = "account_locked_until")
    var accountLockedUntil: Instant? = null,

    // === Groups (for permissions) ===
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "facility_employee_group_members",
        joinColumns = [JoinColumn(name = "employee_id")],
        inverseJoinColumns = [JoinColumn(name = "group_id")]
    )
    var groups: MutableSet<FacilityEmployeeGroup> = mutableSetOf(),

    // === Settings ===
    @Column(length = 50)
    var timezone: String = "UTC",

    @Column(length = 10)
    var locale: String = "en_US"

) : BaseEntity(), UserDetails {

    /**
     * Get full name.
     */
    fun getFullName(): String {
        return "$firstName $lastName"
    }

    /**
     * Check if employee is active.
     */
    fun isActive(): Boolean {
        return status == FacilityEmployeeStatus.ACTIVE
    }

    /**
     * Check if account is locked.
     */
    fun isAccountLocked(): Boolean {
        return accountLockedUntil != null && accountLockedUntil!!.isAfter(Instant.now())
    }

    /**
     * Suspend employee.
     */
    fun suspend(reason: String) {
        this.status = FacilityEmployeeStatus.SUSPENDED
        this.suspendedAt = Instant.now()
        this.suspensionReason = reason
    }

    /**
     * Reactivate suspended employee.
     */
    fun reactivate() {
        if (status == FacilityEmployeeStatus.SUSPENDED) {
            this.status = FacilityEmployeeStatus.ACTIVE
            this.suspendedAt = null
            this.suspensionReason = null
        }
    }

    /**
     * Terminate employment.
     */
    fun terminate(reason: String) {
        this.status = FacilityEmployeeStatus.TERMINATED
        this.terminatedAt = Instant.now()
        this.terminationReason = reason
    }

    /**
     * Record login attempt.
     */
    fun recordLogin(ipAddress: String) {
        this.lastLoginAt = Instant.now()
        this.lastLoginIp = ipAddress
        this.failedLoginAttempts = 0
        this.accountLockedUntil = null
    }

    /**
     * Record failed login attempt.
     */
    fun recordFailedLogin() {
        this.failedLoginAttempts++
        if (failedLoginAttempts >= 5) {
            // Lock account for 30 minutes
            this.accountLockedUntil = Instant.now().plusSeconds(30 * 60)
        }
    }

    /**
     * Check if employee has specific permission.
     */
    fun hasPermission(permission: FacilityPermission): Boolean {
        return groups.any { it.hasPermission(permission) }
    }

    /**
     * Get all permissions from all groups.
     */
    fun getAllPermissions(): Set<FacilityPermission> {
        return groups.flatMap { it.permissions }.toSet()
    }

    // ========== UserDetails Implementation ==========

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return getAllPermissions().map { SimpleGrantedAuthority("FACILITY_${it.name}") }
    }

    override fun getPassword(): String {
        return passwordHash
    }

    override fun getUsername(): String {
        return email
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return !isAccountLocked()
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return status == FacilityEmployeeStatus.ACTIVE
    }

    override fun toString(): String {
        return "FacilityEmployee(id=$id, name='${getFullName()}', email='$email', facility=${facility.name}, status=$status)"
    }
}
