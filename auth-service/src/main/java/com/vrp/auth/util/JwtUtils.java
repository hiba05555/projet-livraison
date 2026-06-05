package com.vrp.auth.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Lightweight JWT utility for reading claims without signature verification.
 *
 * <p><strong>Important:</strong> These methods only Base64-decode the JWT payload.
 * They do NOT validate the signature. Full cryptographic validation is performed
 * by Spring Security's OAuth2 Resource Server (RS256 + JWKS from Keycloak).</p>
 *
 * <p>Use these utilities only to extract claims for business logic (blacklisting,
 * logging) after Spring Security has already authenticated the token.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final ObjectMapper objectMapper;

    /**
     * Extracts the {@code jti} (JWT ID) claim from a raw Bearer token string.
     * Returns empty if the token is malformed.
     */
    public Optional<String> extractJti(String token) {
        return extractClaim(token, "jti");
    }

    /**
     * Extracts the {@code sub} (subject / Keycloak user ID) claim.
     */
    public Optional<String> extractSubject(String token) {
        return extractClaim(token, "sub");
    }

    /**
     * Extracts the {@code preferred_username} claim.
     */
    public Optional<String> extractUsername(String token) {
        return extractClaim(token, "preferred_username");
    }

    /**
     * Extracts the {@code exp} (expiration) claim as an {@link Instant}.
     */
    public Optional<Instant> extractExpiry(String token) {
        return extractClaim(token, "exp")
                .map(exp -> Instant.ofEpochSecond(Long.parseLong(exp)));
    }

    /**
     * Calculates the remaining TTL in seconds from now until token expiry.
     * Returns 0 if already expired or claim is missing.
     */
    public long remainingTtlSeconds(String token) {
        return extractExpiry(token)
                .map(exp -> Math.max(0, exp.getEpochSecond() - Instant.now().getEpochSecond()))
                .orElse(0L);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private Optional<String> extractClaim(String token, String claimName) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();

            byte[] payloadBytes = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            JsonNode payload = objectMapper.readTree(payloadBytes);
            JsonNode claim = payload.get(claimName);
            return claim != null ? Optional.of(claim.asText()) : Optional.empty();
        } catch (Exception e) {
            log.debug("Failed to extract '{}' claim from token: {}", claimName, e.getMessage());
            return Optional.empty();
        }
    }

    /** Adds Base64 padding if missing (JWT tokens omit '=' padding). */
    private String padBase64(String encoded) {
        int mod = encoded.length() % 4;
        return mod == 0 ? encoded : encoded + "=".repeat(4 - mod);
    }
}
