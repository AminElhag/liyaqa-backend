package com.liyaqa.backend.internal.employee.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * Represents an internal team member of Liyaqa.
 * This is completely separate from tenant users - these are our employees
 * who manage and support the platform.
 */
@Entity
@Table(
    name = "internal_employees",
    indexes = [
        Index(name = "idx_employee_email", columnList = "email"),
        Index(name = "idx_employee_status", columnList = "status")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Employee(
    @Column(nullable = false)
    var firstName: String,

    @Column(nullable = false)
    var lastName: String,

    @Column(unique = true, nullable = false)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(unique = true, length = 50)
    var employeeNumber: String? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: EmployeeStatus = EmployeeStatus.ACTIVE,

    @Column(length = 20)
    var phoneNumber: String? = null,

    @Column(nullable = false)
    var department: String,

    @Column(nullable = false)
    var jobTitle: String,

    // Marks system-created accounts (like initial administrator)
    @Column(nullable = false)
    var isSystemAccount: Boolean = false,

    // For support metrics and workload management
    var maxConcurrentTickets: Int = 10,
    var currentActiveTickets: Int = 0,

    // Security fields
    var lastLoginAt: Instant? = null,
    var failedLoginAttempts: Int = 0,
    var lockedUntil: Instant? = null,
    var mustChangePassword: Boolean = false,

    // Relationships
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "employee_groups",
        joinColumns = [JoinColumn(name = "employee_id")],
        inverseJoinColumns = [JoinColumn(name = "group_id")]
    )
    var groups: MutableSet<EmployeeGroup> = mutableSetOf()
) : BaseEntity() {

    // Business methods
    fun getFullName() = "$firstName $lastName"

    fun isAccountLocked(): Boolean =
        lockedUntil?.isAfter(Instant.now()) ?: false

    fun getAllPermissions(): Set<Permission> =
        groups.flatMap { it.permissions }.toSet()

    fun hasPermission(permission: Permission): Boolean =
        getAllPermissions().contains(permission)

    fun hasAnyPermission(vararg permissions: Permission): Boolean =
        permissions.any { hasPermission(it) }
}