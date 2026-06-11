package com.abm.leaseFlow.common.security;

import java.util.UUID;

/**
 * ThreadLocal holder for the current request's tenant ID.
 * Populated by JwtAuthenticationFilter from the JWT claims.
 * Available throughout the request lifecycle — services can call
 * TenantContextHolder.get() without needing it passed as a parameter.
 */
public final class TenantContextHolder {

    private static final InheritableThreadLocal<UUID> CONTEXT = new InheritableThreadLocal<>();

    private TenantContextHolder() {}

    public static void set(UUID tenantId) {
        CONTEXT.set(tenantId);
    }

    public static UUID get() {
        UUID tenantId = CONTEXT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set for current thread");
        }
        return tenantId;
    }

    public static UUID getOrNull() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
