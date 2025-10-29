package com.liyaqa.backend.internal.dto.auth

import java.time.Instant

/**
 * Multi-factor authentication challenge.
 * 
 * Supports our planned 2FA enhancement with various methods.
 */
data class MfaChallenge(
    val challengeId: String,
    val methods: Set<MfaMethod>,
    val expiresAt: Instant
)
