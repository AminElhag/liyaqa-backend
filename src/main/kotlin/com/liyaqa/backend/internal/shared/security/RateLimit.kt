package com.liyaqa.backend.internal.shared.security

/**
 * Rate limiting annotation for preventing abuse.
 *
 * This implements our defense against brute force and DoS attacks
 * at the method level. Rate limits are enforced per user or globally
 * based on configuration.
 *
 * Usage:
 * ```
 * @RateLimit(maxRequests = 5, windowSeconds = 60)
 * @PostMapping("/password-reset")
 * fun requestPasswordReset(...): Response
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(
    val maxRequests: Int,
    val windowSeconds: Int,
    val scope: RateLimitScope = RateLimitScope.USER
)