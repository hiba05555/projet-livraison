package com.vrp.auth.entity;

public enum UserStatus {
    /** Account is active and fully operational. */
    ACTIVE,
    /** Account created but email not yet verified or pending admin approval. */
    PENDING,
    /** Account has been deactivated (soft delete). */
    INACTIVE,
    /** Account is temporarily suspended (policy violation). */
    SUSPENDED
}
