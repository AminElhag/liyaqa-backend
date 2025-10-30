package com.liyaqa.backend.facility.employee.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import com.liyaqa.backend.internal.domain.facility.SportFacility
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * Group of facility employees with shared permissions.
 *
 * Groups allow assigning permissions to multiple employees at once.
 * Examples: "Receptionists", "Coaches", "Facility Managers", "Cleaning Staff"
 *
 * Groups are scoped to a specific facility for multi-tenancy.
 */
@Entity
@Table(
    name = "facility_employee_groups",
    indexes = [
        Index(name = "idx_fac_emp_group_facility", columnList = "facility_id"),
        Index(name = "idx_fac_emp_group_name", columnList = "name")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_facility_group_name", columnNames = ["facility_id", "name"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class FacilityEmployeeGroup(
    // Facility this group belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    var facility: SportFacility,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    // System groups cannot be deleted (e.g., "Administrators", "Staff")
    @Column(name = "is_system", nullable = false)
    var isSystem: Boolean = false,

    // Permissions granted to this group
    @ElementCollection(targetClass = FacilityPermission::class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "facility_employee_group_permissions",
        joinColumns = [JoinColumn(name = "group_id")]
    )
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    var permissions: MutableSet<FacilityPermission> = mutableSetOf()

) : BaseEntity() {

    /**
     * Check if group has specific permission.
     */
    fun hasPermission(permission: FacilityPermission): Boolean {
        return permissions.contains(permission)
    }

    /**
     * Add permission to group.
     */
    fun addPermission(permission: FacilityPermission) {
        permissions.add(permission)
    }

    /**
     * Remove permission from group.
     */
    fun removePermission(permission: FacilityPermission) {
        permissions.remove(permission)
    }

    /**
     * Add multiple permissions.
     */
    fun addPermissions(perms: Collection<FacilityPermission>) {
        permissions.addAll(perms)
    }

    /**
     * Replace all permissions.
     */
    fun setPermissions(perms: Collection<FacilityPermission>) {
        permissions.clear()
        permissions.addAll(perms)
    }

    override fun toString(): String {
        return "FacilityEmployeeGroup(id=$id, name='$name', facility=${facility.name}, permissionCount=${permissions.size})"
    }
}
