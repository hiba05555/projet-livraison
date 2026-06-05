-- =============================================================================
-- V2 – Create audit_logs table
-- =============================================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id                VARCHAR(36)  NOT NULL,
    action            VARCHAR(50)  NOT NULL,
    username          VARCHAR(50),
    keycloak_user_id  VARCHAR(36),
    ip_address        VARCHAR(45),
    details           VARCHAR(500),
    event_timestamp   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_username   ON audit_logs (username);
CREATE INDEX idx_audit_action     ON audit_logs (action);
CREATE INDEX idx_audit_timestamp  ON audit_logs (event_timestamp DESC);
CREATE INDEX idx_audit_kc_user_id ON audit_logs (keycloak_user_id);

COMMENT ON TABLE  audit_logs             IS 'Immutable audit trail – never update or delete rows';
COMMENT ON COLUMN audit_logs.action      IS 'AuditAction enum value';
COMMENT ON COLUMN audit_logs.ip_address  IS 'Client IP (supports IPv6)';
