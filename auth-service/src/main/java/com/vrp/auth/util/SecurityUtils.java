package com.vrp.auth.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility to access the current security context from anywhere in the service layer.
 */
@Component
public class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Returns the Keycloak user ID (JWT {@code sub} claim) of the currently
     * authenticated user, or empty if not authenticated.
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentJwt().map(Jwt::getSubject);
    }

    /**
     * Returns the {@code preferred_username} claim of the authenticated user.
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentJwt()
                .map(jwt -> jwt.getClaimAsString("preferred_username"));
    }

    /**
     * Returns the raw JWT of the current request, if present.
     */
    public static Optional<Jwt> getCurrentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    /**
     * Extracts the client IP address from the HTTP request, handling reverse proxies.
     */
    public static String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
