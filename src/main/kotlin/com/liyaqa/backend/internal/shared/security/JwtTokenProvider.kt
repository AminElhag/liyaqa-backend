package com.liyaqa.backend.internal.shared.security

import com.liyaqa.backend.internal.employee.domain.Employee
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.security.Key
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * JWT token provider with Redis-backed token blacklist.
 *
 * This design balances several concerns:
 * - Security: Short-lived access tokens with longer refresh tokens
 * - Performance: Stateless validation without database hits
 * - Flexibility: Claims-based authorization for fine-grained control
 * - Revocation: Immediate token blacklist via Redis for security
 * - Observability: Comprehensive logging for security monitoring
 *
 * The main trade-off is token size versus database queries. We embed
 * essential claims in the token to avoid database lookups on every request,
 * accepting larger HTTP headers as the cost.
 *
 * Redis Blacklist Implementation:
 * - Keys: `liyaqa:blacklist:token:{tokenHash}`
 * - TTL: Matches token expiration (automatic cleanup)
 * - Performance: O(1) lookup on every request
 * - Distributed: Works across multiple backend instances
 */
@Component
class JwtTokenProvider(
    private val redisTemplate: RedisTemplate<String, Any>,

    @Value("\${liyaqa.security.jwt.secret}")
    private val jwtSecret: String,

    @Value("\${liyaqa.security.jwt.access-token-expiration:3600000}")  // 1 hour default
    private val accessTokenExpiration: Long,

    @Value("\${liyaqa.security.jwt.refresh-token-expiration:604800000}")  // 7 days default
    private val refreshTokenExpiration: Long,

    @Value("\${liyaqa.security.jwt.password-reset-expiration:3600000}")  // 1 hour
    private val passwordResetExpiration: Long,

    @Value("\${liyaqa.session.redis-key-prefix:liyaqa:session:}")
    private val keyPrefix: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Lazy initialization to ensure configuration is loaded
    private val key: Key by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }
    
    companion object {
        const val TOKEN_TYPE_ACCESS = "access"
        const val TOKEN_TYPE_REFRESH = "refresh"
        const val TOKEN_TYPE_PASSWORD_RESET = "password_reset"
        const val TOKEN_TYPE_PASSWORD_CHANGE = "password_change"
        
        // Custom claim keys
        const val CLAIM_TOKEN_TYPE = "token_type"
        const val CLAIM_EMPLOYEE_NAME = "name"
        const val CLAIM_EMPLOYEE_EMAIL = "email"
        const val CLAIM_PERMISSIONS = "permissions"
        const val CLAIM_GROUPS = "groups"
        const val CLAIM_DEPARTMENT = "department"
    }
    
    /**
     * Generates access token with essential claims for request processing.
     * 
     * The design here embeds frequently-needed claims to avoid database
     * queries while keeping the token size reasonable. Permissions are
     * included for fine-grained authorization without round trips.
     */
    fun generateAccessToken(employee: Employee): String {
        val now = Date()
        val expiryDate = Date(now.time + accessTokenExpiration)
        
        // Collect permission names for the token
        val permissions = employee.getAllPermissions().map { it.name }
        val groups = employee.groups.map { it.name }
        
        return Jwts.builder()
            .setSubject(employee.id.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
            .claim(CLAIM_EMPLOYEE_NAME, employee.getFullName())
            .claim(CLAIM_EMPLOYEE_EMAIL, employee.email)
            .claim(CLAIM_PERMISSIONS, permissions)
            .claim(CLAIM_GROUPS, groups)
            .claim(CLAIM_DEPARTMENT, employee.department)
            .signWith(key)
            .compact()
            .also {
                logger.debug("Generated access token for employee ${employee.email}")
            }
    }
    
    /**
     * Generates refresh token with minimal claims for security.
     * 
     * Refresh tokens have fewer claims because they're only used at
     * the refresh endpoint where we can afford a database lookup.
     * This reduces the attack surface if a refresh token is compromised.
     */
    fun generateRefreshToken(employee: Employee): String {
        val now = Date()
        val expiryDate = Date(now.time + refreshTokenExpiration)
        
        return Jwts.builder()
            .setSubject(employee.id.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
            .claim(CLAIM_EMPLOYEE_EMAIL, employee.email)
            .signWith(key)
            .compact()
            .also {
                logger.debug("Generated refresh token for employee ${employee.email}")
            }
    }
    
    /**
     * Generates password reset token with embedded security constraints.
     * 
     * These tokens are single-use and time-limited to reduce the window
     * of opportunity for attacks. The token itself contains no sensitive
     * information, just the user identifier and constraints.
     */
    fun generatePasswordResetToken(employee: Employee): String {
        val now = Date()
        val expiryDate = Date(now.time + passwordResetExpiration)
        
        return Jwts.builder()
            .setSubject(employee.id.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_PASSWORD_RESET)
            .claim(CLAIM_EMPLOYEE_EMAIL, employee.email)
            .setId(UUID.randomUUID().toString()) // Unique ID for single-use tracking
            .signWith(key)
            .compact()
            .also {
                logger.info("Generated password reset token for employee ${employee.email}")
            }
    }
    
    /**
     * Generates limited token for password change flow.
     * 
     * When users must change password (first login, expiry), this token
     * only allows access to the password change endpoint, implementing
     * principle of least privilege.
     */
    fun generatePasswordChangeToken(employee: Employee): String {
        val now = Date()
        val expiryDate = Date(now.time + TimeUnit.HOURS.toMillis(1))
        
        return Jwts.builder()
            .setSubject(employee.id.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_PASSWORD_CHANGE)
            .claim(CLAIM_EMPLOYEE_EMAIL, employee.email)
            .signWith(key)
            .compact()
    }
    
    /**
     * Validates token and extracts claims.
     *
     * This method implements our fail-secure principle - any validation
     * failure returns null rather than throwing, allowing graceful
     * degradation at higher layers.
     *
     * Redis blacklist check happens first for immediate revocation.
     */
    fun validateToken(token: String): Claims? {
        // Check Redis blacklist first for immediate revocation
        if (isTokenBlacklisted(token)) {
            logger.warn("Attempted use of blacklisted token")
            return null
        }

        return try {
            Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (ex: SecurityException) {
            logger.error("Invalid JWT signature: ${ex.message}")
            null
        } catch (ex: MalformedJwtException) {
            logger.error("Invalid JWT token: ${ex.message}")
            null
        } catch (ex: ExpiredJwtException) {
            logger.debug("Expired JWT token: ${ex.message}")
            null
        } catch (ex: UnsupportedJwtException) {
            logger.error("Unsupported JWT token: ${ex.message}")
            null
        } catch (ex: IllegalArgumentException) {
            logger.error("JWT claims string is empty: ${ex.message}")
            null
        }
    }
    
    /**
     * Type-safe token validation for specific token types.
     * 
     * This prevents token confusion attacks where an attacker tries
     * to use one type of token for another purpose.
     */
    fun validateAccessToken(token: String): Claims? {
        val claims = validateToken(token)
        return if (claims?.get(CLAIM_TOKEN_TYPE) == TOKEN_TYPE_ACCESS) {
            claims
        } else {
            logger.warn("Token type mismatch - expected access token")
            null
        }
    }
    
    fun validateRefreshToken(token: String): Claims? {
        val claims = validateToken(token)
        return if (claims?.get(CLAIM_TOKEN_TYPE) == TOKEN_TYPE_REFRESH) {
            claims
        } else {
            logger.warn("Token type mismatch - expected refresh token")
            null
        }
    }
    
    fun validatePasswordResetToken(token: String): Claims? {
        val claims = validateToken(token)
        return if (claims?.get(CLAIM_TOKEN_TYPE) == TOKEN_TYPE_PASSWORD_RESET) {
            claims
        } else {
            logger.warn("Token type mismatch - expected password reset token")
            null
        }
    }
    
    /**
     * Extracts employee ID from token for database lookups.
     */
    fun getEmployeeIdFromToken(token: String): UUID? {
        return validateToken(token)?.let {
            try {
                UUID.fromString(it.subject)
            } catch (ex: IllegalArgumentException) {
                logger.error("Invalid UUID in token subject: ${it.subject}")
                null
            }
        }
    }
    
    /**
     * Blacklists token for immediate revocation with Redis storage.
     *
     * This addresses the main weakness of JWTs - inability to revoke.
     * The blacklist is checked on every request, providing immediate
     * revocation at the cost of a Redis lookup.
     *
     * TTL Strategy:
     * - TTL matches token expiration time
     * - Redis automatically removes expired blacklist entries
     * - No manual cleanup needed
     */
    fun blacklistToken(token: String) {
        try {
            // Calculate remaining token validity for TTL
            val ttlMillis = getTokenExpiration(token)
            if (ttlMillis <= 0) {
                logger.debug("Token already expired, not adding to blacklist")
                return
            }

            // Hash token to prevent storing full JWT (security best practice)
            val tokenHash = token.hashCode().toString()
            val blacklistKey = "${keyPrefix}blacklist:token:$tokenHash"

            // Store in Redis with TTL matching token expiration
            redisTemplate.opsForValue().set(
                blacklistKey,
                true,
                Duration.ofMillis(ttlMillis)
            )

            logger.info("Token blacklisted in Redis (TTL: ${ttlMillis}ms)")
        } catch (ex: Exception) {
            logger.error("Failed to blacklist token in Redis: ${ex.message}", ex)
            // Don't throw - we prefer to continue even if blacklisting fails
            // The token will still expire naturally
        }
    }

    /**
     * Checks if token is blacklisted in Redis.
     *
     * This is called on every request, so it must be fast.
     * Redis provides O(1) lookup performance.
     */
    fun isTokenBlacklisted(token: String): Boolean {
        return try {
            val tokenHash = token.hashCode().toString()
            val blacklistKey = "${keyPrefix}blacklist:token:$tokenHash"
            val isBlacklisted = redisTemplate.hasKey(blacklistKey)

            if (isBlacklisted) {
                logger.debug("Token found in blacklist")
            }

            isBlacklisted
        } catch (ex: Exception) {
            logger.error("Failed to check token blacklist in Redis: ${ex.message}", ex)
            // Fail open - if Redis is down, allow the request
            // The token will still be validated for signature and expiration
            false
        }
    }
    
    /**
     * Gets remaining token validity for monitoring and UI feedback.
     */
    fun getTokenExpiration(token: String): Long {
        return validateToken(token)?.let {
            it.expiration.time - System.currentTimeMillis()
        } ?: 0
    }
    
    fun getAccessTokenExpiration(): Long = accessTokenExpiration
    fun getRefreshTokenExpiration(): Long = refreshTokenExpiration
}
