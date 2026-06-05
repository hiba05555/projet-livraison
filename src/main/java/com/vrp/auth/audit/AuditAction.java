package com.vrp.auth.audit;

/**
 * Enumeration of all auditable events in the auth-service.
 * Each constant is stored as-is in the {@code audit_logs} table.
 */
public enum AuditAction {
    USER_REGISTERED,
    USER_LOGGED_OUT,
    USER_PROFILE_UPDATED,
    ROLE_ASSIGNED,
    USER_SUSPENDED,
    USER_ACTIVATED,
    USER_DELETED,
    PASSWORD_RESET_REQUESTED,
    TOKEN_BLACKLISTED,
    UNAUTHORIZED_ACCESS_ATTEMPT
}
