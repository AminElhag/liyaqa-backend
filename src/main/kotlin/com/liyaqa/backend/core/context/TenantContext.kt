package com.liyaqa.backend.core.context

/**
 * Thread-local storage for tenant context.
 * This allows us to maintain tenant isolation throughout
 * the request lifecycle without passing tenant ID through
 * every method signature.
 */
object TenantContext {
    private val currentTenant = ThreadLocal<String>()

    fun setTenantId(tenantId: String) {
        currentTenant.set(tenantId)
    }

    fun getTenantId(): String {
        return currentTenant.get()
            ?: throw IllegalStateException("No tenant context available")
    }

    fun clear() {
        currentTenant.remove()
    }

    fun hasTenant(): Boolean {
        return currentTenant.get() != null
    }
}