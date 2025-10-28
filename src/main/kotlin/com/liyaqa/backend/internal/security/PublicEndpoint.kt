package com.liyaqa.backend.internal.security
/**
 * Marks an endpoint as public (no authentication required).
 * 
 * This makes the security stance explicit - we have to consciously
 * mark something as public rather than accidentally leaving it open.
 * The annotation serves as documentation and a security review trigger.
 * 
 * Usage:
 * ```
 * @PublicEndpoint
 * @GetMapping("/health")
 * fun healthCheck(): HealthStatus
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicEndpoint
