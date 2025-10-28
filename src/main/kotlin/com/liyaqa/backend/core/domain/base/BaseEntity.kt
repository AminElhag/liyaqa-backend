package com.liyaqa.backend.core.domain.base

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

/**
 * Base entity providing common fields for all domain entities.
 * This ensures consistent audit trails and tenant isolation across
 * our entire data model.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "tenant_id", nullable = false)
    var tenantId: String = ""

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()

    @Version
    var version: Long = 0
}