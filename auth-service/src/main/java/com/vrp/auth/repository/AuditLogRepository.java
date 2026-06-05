package com.vrp.auth.repository;

import com.vrp.auth.audit.AuditAction;
import com.vrp.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByUsernameOrderByEventTimestampDesc(String username);

    List<AuditLog> findByKeycloakUserIdOrderByEventTimestampDesc(String keycloakUserId);

    Page<AuditLog> findByActionOrderByEventTimestampDesc(AuditAction action, Pageable pageable);

    List<AuditLog> findByEventTimestampBetweenOrderByEventTimestampDesc(Instant from, Instant to);
}
