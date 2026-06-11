package com.abm.leaseFlow.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Persists audit records asynchronously, AFTER the originating transaction commits.
 *
 * Using @TransactionalEventListener(AFTER_COMMIT) guarantees:
 *   1. Audit writes never block or slow the main transaction.
 *   2. Audit records only appear if the business operation succeeded.
 *   3. A failure in audit writing does NOT roll back the business operation.
 *
 * Using @Async ensures the audit write runs on a separate thread
 * so it doesn't execute inside the HTTP request thread after commit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditEvent(AuditEvent event) {
        try {
            AuditLog log = AuditLog.builder()
                    .tenantId(event.getTenantId())
                    .entityType(event.getEntityType())
                    .entityId(event.getEntityId())
                    .action(event.getAction())
                    .performedBy(event.getPerformedBy())
                    .performedAt(Instant.now())
                    .ipAddress(event.getIpAddress())
                    .details(event.getDetails())
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Never let audit failures bubble up and affect the caller
            AuditService.log.error("Failed to persist audit log [action={}]: {}",
                    event.getAction(), e.getMessage(), e);
        }
    }
}
