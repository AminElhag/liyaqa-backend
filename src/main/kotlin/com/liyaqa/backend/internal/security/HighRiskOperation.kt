package com.liyaqa.backend.internal.security
/**
 * Marks an operation as high-risk requiring additional audit logging.
 * 
 * This triggers enhanced logging and potentially additional security
 * checks like 2FA confirmation. It's part of our risk-based security
 * model where we apply additional controls based on operation sensitivity.
 * 
 * Usage:
 * ```
 * @HighRiskOperation(reason = "Modifies payment settings")
 * fun updatePaymentConfiguration(...): PaymentConfig
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HighRiskOperation(
    val reason: String,
    val requireReauth: Boolean = false,
    val require2FA: Boolean = false
)
