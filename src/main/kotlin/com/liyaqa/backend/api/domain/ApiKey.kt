package com.liyaqa.backend.api.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * API Key entity for Public API authentication.
 *
 * Design Philosophy:
 * "An API transforms us from a closed system to a platform."
 *
 * API keys enable external integrations without exposing internal authentication.
 * Each key has specific scopes/permissions and rate limits.
 *
 * Security Considerations:
 * - Keys are hashed (only prefix stored in plain text for identification)
 * - Rate limited per key
 * - Expirable
 * - Revokable
 * - Scoped to specific permissions
 */
@Entity
@Table(
    name = "api_keys",
    indexes = [
        Index(name = "idx_api_key_tenant", columnList = "tenant_id"),
        Index(name = "idx_api_key_prefix", columnList = "key_prefix"),
        Index(name = "idx_api_key_status", columnList = "status"),
        Index(name = "idx_api_key_facility", columnList = "facility_id")
    ]
)
class ApiKey(
    // Key Information
    @Column(name = "key_prefix", length = 20, nullable = false)
    var keyPrefix: String, // First 8 chars for identification (e.g., "lyk_live_")

    @Column(name = "key_hash", length = 255, nullable = false)
    var keyHash: String, // BCrypt hash of full key

    @Column(name = "name", length = 255, nullable = false)
    var name: String, // Human-readable name (e.g., "Mobile App Integration")

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    // Ownership
    @Column(name = "facility_id", nullable = false)
    var facilityId: UUID,

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    var status: ApiKeyStatus = ApiKeyStatus.ACTIVE,

    // Permissions (JSON array of scopes)
    @Column(name = "scopes", columnDefinition = "TEXT", nullable = false)
    var scopes: String, // JSON array: ["bookings:read", "bookings:write", "members:read", etc.]

    // Rate Limiting
    @Column(name = "rate_limit_per_hour")
    var rateLimitPerHour: Int = 1000,

    @Column(name = "rate_limit_per_day")
    var rateLimitPerDay: Int = 10000,

    // Expiration
    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    // Usage Tracking
    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,

    @Column(name = "total_requests")
    var totalRequests: Long = 0,

    // Environment
    @Enumerated(EnumType.STRING)
    @Column(name = "environment", length = 20, nullable = false)
    var environment: ApiKeyEnvironment = ApiKeyEnvironment.LIVE,

    // Metadata
    @Column(name = "created_by_id")
    var createdById: UUID? = null,

    @Column(name = "created_by_name", length = 255)
    var createdByName: String? = null,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "revoked_by_id")
    var revokedById: UUID? = null,

    @Column(name = "revoked_reason", columnDefinition = "TEXT")
    var revokedReason: String? = null

) : BaseEntity() {

    /**
     * Check if API key is currently valid.
     */
    fun isValid(): Boolean {
        if (status != ApiKeyStatus.ACTIVE) return false
        if (expiresAt != null && expiresAt!!.isBefore(Instant.now())) return false
        return true
    }

    /**
     * Check if API key has a specific scope.
     */
    fun hasScope(scope: String): Boolean {
        // Parse JSON array of scopes
        return scopes.contains(scope)
    }

    /**
     * Record API key usage.
     */
    fun recordUsage() {
        this.lastUsedAt = Instant.now()
        this.totalRequests++
    }

    /**
     * Revoke the API key.
     */
    fun revoke(reason: String, revokedBy: UUID? = null) {
        this.status = ApiKeyStatus.REVOKED
        this.revokedAt = Instant.now()
        this.revokedById = revokedBy
        this.revokedReason = reason
    }
}

/**
 * API Key status.
 */
enum class ApiKeyStatus {
    ACTIVE,      // Key is active and can be used
    INACTIVE,    // Key is temporarily disabled
    REVOKED,     // Key has been permanently revoked
    EXPIRED      // Key has expired
}

/**
 * API Key environment.
 */
enum class ApiKeyEnvironment {
    TEST,   // Test/sandbox environment
    LIVE    // Production environment
}

/**
 * Standard API scopes for permission management.
 */
object ApiScopes {
    // Facility scopes
    const val FACILITIES_READ = "facilities:read"

    // Court scopes
    const val COURTS_READ = "courts:read"

    // Booking scopes
    const val BOOKINGS_READ = "bookings:read"
    const val BOOKINGS_WRITE = "bookings:write"

    // Member scopes
    const val MEMBERS_READ = "members:read"
    const val MEMBERS_WRITE = "members:write"

    // Payment scopes
    const val PAYMENTS_READ = "payments:read"
    const val PAYMENTS_WRITE = "payments:write"

    // Webhook scopes
    const val WEBHOOKS_READ = "webhooks:read"
    const val WEBHOOKS_WRITE = "webhooks:write"

    /**
     * Get all available scopes.
     */
    fun getAllScopes(): List<String> = listOf(
        FACILITIES_READ,
        COURTS_READ,
        BOOKINGS_READ,
        BOOKINGS_WRITE,
        MEMBERS_READ,
        MEMBERS_WRITE,
        PAYMENTS_READ,
        PAYMENTS_WRITE,
        WEBHOOKS_READ,
        WEBHOOKS_WRITE
    )
}
