package com.liyaqa.backend.internal.domain.employee

import com.liyaqa.backend.core.domain.base.BaseEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

/**
 * Groups represent roles within our internal team structure.
 * This design allows us to flexibly assign permissions without
 * hardcoding roles, supporting our growth as team structures evolve.
 */
@Entity
@Table(name = "internal_employee_groups")
@EntityListeners(AuditingEntityListener::class)
class EmployeeGroup(
    @Column(unique = true, nullable = false)
    var name: String,
    
    @Column(nullable = false)
    var description: String,
    
    @Column(nullable = false)
    var isSystem: Boolean = false, // System groups can't be deleted
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "group_permissions",
        joinColumns = [JoinColumn(name = "group_id")]
    )
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    var permissions: MutableSet<Permission> = mutableSetOf()
) : BaseEntity ()