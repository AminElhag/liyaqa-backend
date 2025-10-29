package com.liyaqa.backend.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Configuration

/**
 * Multi-tenancy configuration establishing our tenant isolation strategy.
 * We're using a shared database with row-level security initially,
 * which balances simplicity with security for our early-stage SaaS.
 */
@Configuration
@ConfigurationPropertiesScan
class MultiTenancyConfig {

    companion object {
        const val TENANT_HEADER = "X-Tenant-Id"
        const val DEFAULT_TENANT = "system"
    }
}

@ConfigurationProperties(prefix = "liyaqa.multitenancy")
data class MultiTenancyProperties(
    val enabled: Boolean = true,
    val strategy: TenantStrategy = TenantStrategy.DISCRIMINATOR,
    val headerName: String = MultiTenancyConfig.TENANT_HEADER
)

enum class TenantStrategy {
    DISCRIMINATOR,  // Row-level isolation (our starting point)
    SCHEMA,         // Schema per tenant (future enhancement)
    DATABASE        // Database per tenant (enterprise tier)
}