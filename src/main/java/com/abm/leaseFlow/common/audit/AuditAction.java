package com.abm.leaseFlow.common.audit;

/**
 * Canonical audit action constants used across all modules.
 */
public final class AuditAction {

    private AuditAction() {}

    // Auth
    public static final String TENANT_REGISTERED = "TENANT_REGISTERED";
    public static final String USER_CREATED      = "USER_CREATED";
    public static final String LOGIN_SUCCESS     = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED      = "LOGIN_FAILED";
    public static final String LOGOUT            = "LOGOUT";
    public static final String TOKEN_REFRESHED   = "TOKEN_REFRESHED";

    // Lease lifecycle (future modules)
    public static final String LEASE_CREATED     = "LEASE_CREATED";
    public static final String LEASE_GENERATED   = "LEASE_GENERATED";
    public static final String LEASE_SENT        = "LEASE_SENT";
    public static final String LEASE_SIGNED      = "LEASE_SIGNED";
    public static final String LEASE_ACTIVATED   = "LEASE_ACTIVATED";
    public static final String LEASE_TERMINATED  = "LEASE_TERMINATED";
}
