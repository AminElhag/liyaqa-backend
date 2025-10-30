package com.liyaqa.backend.internal.auth.service

import com.liyaqa.backend.internal.employee.domain.Employee
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Session management service with distributed Redis storage.
 *
 * This implementation provides enterprise-grade session management:
 * - Distributed storage enabling horizontal scaling
 * - Automatic TTL-based expiration (no manual cleanup needed)
 * - Multi-key indexing for efficient lookups
 * - Token reuse detection for security
 *
 * Redis Key Structure:
 * - `session:token:{refreshToken}` - Primary lookup by refresh token
 * - `session:id:{sessionId}` - Lookup by session ID
 * - `session:employee:{employeeId}:{sessionId}` - Employee's sessions index
 * - `session:ip:{employeeId}:{ipAddress}` - IP history for risk scoring
 *
 * Design Trade-offs:
 * - Network latency vs. scalability (acceptable for session ops)
 * - Multiple keys per session vs. query flexibility (worth it for performance)
 * - Memory usage vs. immediate revocation capability (critical for security)
 */
@Service
class SessionService(
    private val redisTemplate: RedisTemplate<String, Any>,

    @Value("\${liyaqa.session.timeout-hours:8}")
    private val sessionTimeoutHours: Long,

    @Value("\${liyaqa.session.redis-key-prefix:liyaqa:session:}")
    private val keyPrefix: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    data class Session(
        val id: String,
        val employeeId: UUID,
        val refreshToken: String,
        val ipAddress: String,
        val userAgent: String?,
        val createdAt: Instant,
        val lastActivityAt: Instant,
        var refreshTokenUsed: Boolean = false
    ) {
        fun isActive(): Boolean = lastActivityAt.isAfter(
            Instant.now().minus(8, ChronoUnit.HOURS)
        )
    }

    /**
     * Creates a new session with Redis storage and multiple indexes.
     *
     * Stores session with three keys for different access patterns:
     * 1. By token (primary lookup during refresh)
     * 2. By session ID (for direct termination)
     * 3. By employee ID (for revoke all sessions)
     */
    fun createSession(
        employee: Employee,
        ipAddress: String,
        userAgent: String?,
        refreshToken: String
    ): Session {
        val session = Session(
            id = UUID.randomUUID().toString(),
            employeeId = employee.id!!,
            refreshToken = refreshToken,
            ipAddress = ipAddress,
            userAgent = userAgent,
            createdAt = Instant.now(),
            lastActivityAt = Instant.now()
        )

        val timeout = Duration.ofHours(sessionTimeoutHours)

        try {
            // Primary key: lookup by refresh token (most common operation)
            val tokenKey = "${keyPrefix}token:${refreshToken}"
            redisTemplate.opsForValue().set(tokenKey, session, timeout)

            // Secondary key: lookup by session ID (for termination)
            val sessionKey = "${keyPrefix}id:${session.id}"
            redisTemplate.opsForValue().set(sessionKey, session, timeout)

            // Index key: track employee's sessions (for revoke all)
            val employeeKey = "${keyPrefix}employee:${employee.id}:${session.id}"
            redisTemplate.opsForValue().set(employeeKey, session.id, timeout)

            // IP history key: track known IPs for risk scoring
            val ipKey = "${keyPrefix}ip:${employee.id}:${ipAddress}"
            redisTemplate.opsForValue().set(ipKey, true, Duration.ofDays(30))

            logger.info("Session created in Redis for ${employee.email}: ${session.id} (TTL: ${timeout.toHours()}h)")
        } catch (ex: Exception) {
            logger.error("Failed to create session in Redis: ${ex.message}", ex)
            throw SessionStorageException("Failed to create session", ex)
        }

        return session
    }

    /**
     * Finds session by refresh token (primary lookup method).
     *
     * This is called on every token refresh operation, so it's optimized
     * for speed with a single Redis GET operation.
     */
    fun findByRefreshToken(refreshToken: String): Session? {
        return try {
            val tokenKey = "${keyPrefix}token:${refreshToken}"
            val session = redisTemplate.opsForValue().get(tokenKey) as? Session

            if (session != null) {
                logger.debug("Session found for token: ${session.id}")
            } else {
                logger.debug("No session found for refresh token")
            }

            session
        } catch (ex: Exception) {
            logger.error("Failed to lookup session by token: ${ex.message}", ex)
            null
        }
    }

    /**
     * Rotates refresh token with token reuse detection.
     *
     * Implementation:
     * 1. Mark old token as used (security)
     * 2. Delete old token key
     * 3. Create new token key with updated session
     * 4. Update session ID key
     *
     * If old token is reused after this, it won't be found and will
     * trigger security breach handling in AuthenticationService.
     */
    fun rotateRefreshToken(sessionId: String, oldToken: String, newToken: String) {
        try {
            // Get current session
            val sessionKey = "${keyPrefix}id:${sessionId}"
            val session = redisTemplate.opsForValue().get(sessionKey) as? Session

            if (session == null) {
                logger.warn("Cannot rotate token - session not found: $sessionId")
                return
            }

            // Mark old token as used (for security audit)
            val markedSession = session.copy(refreshTokenUsed = true)
            val oldTokenKey = "${keyPrefix}token:${oldToken}"
            redisTemplate.opsForValue().set(oldTokenKey, markedSession, Duration.ofMinutes(5))

            // Create new session with new token
            val newSession = session.copy(
                refreshToken = newToken,
                refreshTokenUsed = false,
                lastActivityAt = Instant.now()
            )

            val timeout = Duration.ofHours(sessionTimeoutHours)

            // Store with new token key
            val newTokenKey = "${keyPrefix}token:${newToken}"
            redisTemplate.opsForValue().set(newTokenKey, newSession, timeout)

            // Update session ID key
            redisTemplate.opsForValue().set(sessionKey, newSession, timeout)

            // Update employee index
            val employeeKey = "${keyPrefix}employee:${session.employeeId}:${sessionId}"
            redisTemplate.expire(employeeKey, timeout)

            logger.debug("Refresh token rotated for session $sessionId in Redis")
        } catch (ex: Exception) {
            logger.error("Failed to rotate refresh token: ${ex.message}", ex)
            throw SessionStorageException("Failed to rotate token", ex)
        }
    }

    /**
     * Terminates a single session by removing all its keys.
     */
    fun terminateSession(sessionId: String) {
        try {
            // Get session to find all its keys
            val sessionKey = "${keyPrefix}id:${sessionId}"
            val session = redisTemplate.opsForValue().get(sessionKey) as? Session

            if (session != null) {
                // Delete all keys associated with this session
                val tokenKey = "${keyPrefix}token:${session.refreshToken}"
                val employeeKey = "${keyPrefix}employee:${session.employeeId}:${sessionId}"

                redisTemplate.delete(listOf(sessionKey, tokenKey, employeeKey))
                logger.info("Session terminated in Redis: $sessionId")
            } else {
                logger.warn("Session not found for termination: $sessionId")
            }
        } catch (ex: Exception) {
            logger.error("Failed to terminate session: ${ex.message}", ex)
            throw SessionStorageException("Failed to terminate session", ex)
        }
    }

    /**
     * Revokes all sessions for an employee (security breach scenario).
     *
     * Uses the employee index to efficiently find and delete all sessions.
     */
    fun revokeAllSessions(employeeId: UUID) {
        try {
            // Find all session keys for this employee
            val pattern = "${keyPrefix}employee:${employeeId}:*"
            val employeeKeys = redisTemplate.keys(pattern)

            if (employeeKeys.isEmpty()) {
                logger.info("No sessions to revoke for employee $employeeId")
                return
            }

            // For each session, delete all associated keys
            val allKeysToDelete = mutableListOf<String>()
            employeeKeys.forEach { employeeKey ->
                val sessionId = redisTemplate.opsForValue().get(employeeKey) as? String
                if (sessionId != null) {
                    val sessionKey = "${keyPrefix}id:${sessionId}"
                    val session = redisTemplate.opsForValue().get(sessionKey) as? Session
                    if (session != null) {
                        val tokenKey = "${keyPrefix}token:${session.refreshToken}"
                        allKeysToDelete.addAll(listOf(sessionKey, tokenKey, employeeKey))
                    }
                }
            }

            // Delete all keys in batch
            if (allKeysToDelete.isNotEmpty()) {
                redisTemplate.delete(allKeysToDelete)
            }

            logger.warn("All sessions revoked in Redis for employee $employeeId (${employeeKeys.size} sessions)")
        } catch (ex: Exception) {
            logger.error("Failed to revoke all sessions: ${ex.message}", ex)
            throw SessionStorageException("Failed to revoke sessions", ex)
        }
    }

    /**
     * Checks if employee has previously logged in from this IP.
     *
     * Used for risk scoring in authentication flow. Returns true if
     * this is a known IP address for this employee.
     */
    fun hasLoginFromIp(employeeId: UUID, ipAddress: String): Boolean {
        return try {
            val ipKey = "${keyPrefix}ip:${employeeId}:${ipAddress}"
            val exists = redisTemplate.hasKey(ipKey)
            logger.debug("IP check for $employeeId from $ipAddress: ${if (exists) "known" else "new"}")
            exists
        } catch (ex: Exception) {
            logger.error("Failed to check IP history: ${ex.message}", ex)
            false // Fail open for risk scoring
        }
    }
}

/**
 * Exception thrown when Redis session operations fail.
 */
class SessionStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)