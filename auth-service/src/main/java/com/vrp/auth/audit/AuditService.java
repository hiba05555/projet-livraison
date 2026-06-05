package com.vrp.auth.audit;

import com.vrp.auth.entity.AuditLog;
import com.vrp.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asynchronous audit logging service.
 *
 * <p>All writes are performed in a <strong>new transaction</strong> so that
 * audit records are persisted even when the calling transaction rolls back.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Persists an audit event asynchronously.
     * Uses a new transaction to ensure the log is written regardless of
     * the caller's transaction outcome.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String username, String keycloakUserId,
                    String ipAddress, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .username(username)
                    .keycloakUserId(keycloakUserId)
                    .ipAddress(ipAddress)
                    .details(details)
                    .build();

            auditLogRepository.save(entry);
            log.debug("Audit: [{}] user={} ip={}", action, username, ipAddress);
        } catch (Exception e) {
            // Audit failures must not break the main flow
            log.error("Failed to write audit log for action {}: {}", action, e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String username, String keycloakUserId, String details) {
        log(action, username, keycloakUserId, null, details);
    }
}
