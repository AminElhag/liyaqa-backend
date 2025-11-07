package com.liyaqa.backend.api.service

import com.liyaqa.backend.api.data.ApiKeyRepository
import com.liyaqa.backend.api.domain.ApiKey
import com.liyaqa.backend.api.domain.ApiKeyEnvironment
import com.liyaqa.backend.api.domain.ApiKeyStatus
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.*

/**
 * Service for managing API keys.
 *
 * Design Philosophy:
 * API keys are the gateway to our platform. They must be:
 * - Secure (hashed, not stored in plain text)
 * - Identifiable (prefix for easy recognition)
 * - Manageable (revokable, expirable)
 * - Auditable (track usage and access)
 */
@Service
@Transactional
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureRandom = SecureRandom()

    companion object {
        private const val KEY_LENGTH = 32
        private const val PREFIX_LENGTH = 8
    }

    /**
     * Generate a new API key.
     *
     * @return Pair of (ApiKey entity, raw key string). Raw key is only returned once!
     */
    fun generateApiKey(
        tenantId: String,
        facilityId: UUID,
        name: String,
        description: String?,
        scopes: List<String>,
        environment: ApiKeyEnvironment = ApiKeyEnvironment.LIVE,
        rateLimitPerHour: Int = 1000,
        rateLimitPerDay: Int = 10000,
        expiresAt: java.time.Instant? = null,
        createdById: UUID? = null,
        createdByName: String? = null
    ): Pair<ApiKey, String> {
        // Generate random key
        val rawKey = generateRandomKey(environment)
        val keyPrefix = rawKey.substring(0, PREFIX_LENGTH)
        val keyHash = passwordEncoder.encode(rawKey)

        // Create API key entity
        val apiKey = ApiKey(
            keyPrefix = keyPrefix,
            keyHash = keyHash,
            name = name,
            description = description,
            facilityId = facilityId,
            scopes = scopes.joinToString(","),
            rateLimitPerHour = rateLimitPerHour,
            rateLimitPerDay = rateLimitPerDay,
            expiresAt = expiresAt,
            environment = environment,
            createdById = createdById,
            createdByName = createdByName
        )

        apiKey.tenantId = tenantId

        val saved = apiKeyRepository.save(apiKey)

        logger.info(
            "Generated API key: id={}, name={}, facility={}, scopes={}",
            saved.id, name, facilityId, scopes
        )

        return Pair(saved, rawKey)
    }

    /**
     * Validate API key and return associated entity.
     */
    fun validateApiKey(rawKey: String): ApiKey? {
        if (rawKey.length < PREFIX_LENGTH) return null

        val prefix = rawKey.substring(0, PREFIX_LENGTH)
        val apiKey = apiKeyRepository.findActiveByPrefix(prefix) ?: return null

        // Verify key hash
        if (!passwordEncoder.matches(rawKey, apiKey.keyHash)) {
            logger.warn("Invalid API key attempt: prefix={}", prefix)
            return null
        }

        // Check validity
        if (!apiKey.isValid()) {
            logger.warn("API key no longer valid: id={}, status={}", apiKey.id, apiKey.status)
            return null
        }

        // Record usage
        apiKey.recordUsage()
        apiKeyRepository.save(apiKey)

        return apiKey
    }

    /**
     * Revoke an API key.
     */
    fun revokeApiKey(
        apiKeyId: UUID,
        reason: String,
        revokedBy: UUID? = null
    ): ApiKey? {
        val apiKey = apiKeyRepository.findById(apiKeyId).orElse(null) ?: return null

        apiKey.revoke(reason, revokedBy)
        val revoked = apiKeyRepository.save(apiKey)

        logger.info(
            "Revoked API key: id={}, name={}, reason={}",
            revoked.id, revoked.name, reason
        )

        return revoked
    }

    /**
     * List API keys for a facility.
     */
    fun listApiKeys(facilityId: UUID): List<ApiKey> {
        return apiKeyRepository.findByFacilityIdOrderByCreatedAtDesc(facilityId)
    }

    /**
     * Get API key by ID.
     */
    fun getApiKey(apiKeyId: UUID): ApiKey? {
        return apiKeyRepository.findById(apiKeyId).orElse(null)
    }

    /**
     * Generate a random API key string.
     */
    private fun generateRandomKey(environment: ApiKeyEnvironment): String {
        val prefix = when (environment) {
            ApiKeyEnvironment.TEST -> "lyk_test_"
            ApiKeyEnvironment.LIVE -> "lyk_live_"
        }

        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val keyBytes = ByteArray(KEY_LENGTH)
        secureRandom.nextBytes(keyBytes)

        val randomPart = keyBytes.map { chars[Math.abs(it.toInt()) % chars.length] }.joinToString("")

        return prefix + randomPart
    }

    /**
     * Check if API key has required scope.
     */
    fun checkScope(apiKey: ApiKey, requiredScope: String): Boolean {
        return apiKey.hasScope(requiredScope)
    }
}
