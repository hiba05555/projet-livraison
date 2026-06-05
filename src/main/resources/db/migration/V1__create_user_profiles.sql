-- =============================================================================
-- V1 – Create user_profiles table
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_profiles (
    id                VARCHAR(36)  NOT NULL,
    keycloak_user_id  VARCHAR(36)  NOT NULL,
    username          VARCHAR(50)  NOT NULL,
    email             VARCHAR(100) NOT NULL,
    first_name        VARCHAR(50),
    last_name         VARCHAR(50),
    phone_number      VARCHAR(20),
    role              VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_profiles PRIMARY KEY (id),
    CONSTRAINT uq_user_profiles_keycloak_id UNIQUE (keycloak_user_id),
    CONSTRAINT uq_user_profiles_email       UNIQUE (email),
    CONSTRAINT uq_user_profiles_username    UNIQUE (username),
    CONSTRAINT chk_user_profiles_role   CHECK (role   IN ('ADMIN','DRIVER','USER')),
    CONSTRAINT chk_user_profiles_status CHECK (status IN ('ACTIVE','PENDING','INACTIVE','SUSPENDED'))
);

CREATE INDEX idx_user_profiles_role   ON user_profiles (role);
CREATE INDEX idx_user_profiles_status ON user_profiles (status);

COMMENT ON TABLE  user_profiles                   IS 'VRP-domain mirror of Keycloak users';
COMMENT ON COLUMN user_profiles.keycloak_user_id  IS 'Keycloak subject (sub claim) – primary FK to Keycloak';
COMMENT ON COLUMN user_profiles.version           IS 'JPA optimistic locking version';
