package com.abm.leaseFlow.common.audit;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * Domain event published whenever an auditable action occurs.
 * The AuditService listens with @TransactionalEventListener(AFTER_COMMIT)
 * so audit writes never block or roll back the main transaction.
 */
@Getter
@Builder
public class AuditEvent {

    private final UUID              tenantId;
    private final String            entityType;
    private final UUID              entityId;
    private final String            action;
    private final UUID              performedBy;
    private final String            ipAddress;
    private final Map<String, Object> details;
}
