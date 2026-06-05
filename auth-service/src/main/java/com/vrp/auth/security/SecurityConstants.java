package com.vrp.auth.security;

/**
 * Constantes de sécurité centralisées.
 *
 * <p>Le login n'apparaît pas dans les endpoints publics car il est géré
 * directement par Keycloak (Authorization Code Flow + PKCE).
 * L'auth-service ne joue aucun rôle dans le flux de login.</p>
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    // ── JWT Claims ────────────────────────────────────────────────────────
    public static final String CLAIM_REALM_ACCESS   = "realm_access";
    public static final String CLAIM_ROLES          = "roles";
    public static final String CLAIM_PREFERRED_USER = "preferred_username";
    public static final String CLAIM_EMAIL          = "email";
    public static final String CLAIM_JTI            = "jti";

    // ── Rôles VRP ─────────────────────────────────────────────────────────
    public static final String ROLE_ADMIN  = "ADMIN";
    public static final String ROLE_DRIVER = "DRIVER";
    public static final String ROLE_USER   = "USER";

    // Spring Security exige le préfixe ROLE_ pour hasRole()
    public static final String SPRING_ROLE_PREFIX = "ROLE_";

    // ── Endpoints publics (sans JWT) ──────────────────────────────────────
    // Le login N'EST PAS ici — il se fait directement sur Keycloak via PKCE.
    public static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/register",
        "/api/v1/auth/password-reset",
        "/api/v1/auth/chauffeurs/disponibles",  // interne : appelé par vrp-service
        "/actuator/health",
        "/actuator/info",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api-docs/**",
        "/v3/api-docs/**"
    };

    // ── HTTP header ───────────────────────────────────────────────────────
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX        = "Bearer ";
}
