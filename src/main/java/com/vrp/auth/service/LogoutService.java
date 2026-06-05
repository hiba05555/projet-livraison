package com.vrp.auth.service;

import com.vrp.auth.audit.AuditAction;
import com.vrp.auth.audit.AuditService;
import com.vrp.auth.event.UserLoggedOutEvent;
import com.vrp.auth.redis.TokenBlacklistService;
import com.vrp.auth.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Handles secure logout by blacklisting the current access token and
 * optionally the refresh token in Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final TokenBlacklistService blacklistService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Blacklists both the access token and (if provided) the refresh token.
     *
     * @param jwt          the validated JWT of the current request
     * @param accessToken  raw access token string
     * @param refreshToken optional raw refresh token string
     * @param ipAddress    client IP for audit
     */
    public void logout(Jwt jwt, String accessToken, String refreshToken, String ipAddress) {
        String keycloakUserId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");

        // ── 1. Blacklist access token ──────────────────────────────────────
        blacklistService.blacklist(accessToken);
        log.info("Access token blacklisted for user '{}'", username);

        // ── 2. Blacklist refresh token if provided ─────────────────────────
        if (refreshToken != null && !refreshToken.isBlank()) {
            blacklistService.blacklist(refreshToken);
            log.info("Refresh token blacklisted for user '{}'", username);
        }

        // ── 3. Publish event ───────────────────────────────────────────────
        eventPublisher.publishEvent(
            new UserLoggedOutEvent(this, keycloakUserId, username, ipAddress)
        );

        // ── 4. Audit ───────────────────────────────────────────────────────
        auditService.log(
            AuditAction.USER_LOGGED_OUT,
            username,
            keycloakUserId,
            ipAddress,
            null
        );
    }
}
