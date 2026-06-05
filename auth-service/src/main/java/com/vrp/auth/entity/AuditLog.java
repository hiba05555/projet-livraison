package com.vrp.auth.entity;

import com.vrp.auth.audit.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Immutable audit trail for all authentication and authorisation events.
 * Records are append-only; never update or delete audit logs.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_username",  columnList = "username"),
    @Index(name = "idx_audit_action",    columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "event_timestamp")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "keycloak_user_id", length = 36)
    private String keycloakUserId;

    /** Client IP address for forensic analysis. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Free-text context (role assigned, error message, etc.). */
    @Column(name = "details", length = 500)
    private String details;

    @CreatedDate
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private Instant eventTimestamp;
}
