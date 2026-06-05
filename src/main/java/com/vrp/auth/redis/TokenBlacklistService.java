package com.vrp.auth.redis;

import com.vrp.auth.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed token blacklist for secure logout.
 *
 * <p>Strategy: store the JWT ID ({@code jti} claim) with a TTL equal to the token's
 * remaining lifetime. This ensures Redis is self-cleaning and does not grow unbounded.</p>
 *
 * <p>The {@link TokenBlacklistFilter} checks this service on every incoming request
 * before Spring Security processes the JWT.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:jti:";
    private static final String REVOKED_VALUE        = "REVOKED";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtils jwtUtils;

    /**
     * Adds a token's JTI to the blacklist.
     * TTL is set to the token's remaining lifetime so Redis is self-cleaning.
     *
     * @param rawToken the raw JWT Bearer token string
     */
    public void blacklist(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;

        String jti = jwtUtils.extractJti(rawToken).orElse(null);
        if (jti == null) {
            log.warn("Cannot blacklist token: no JTI claim found");
            return;
        }

        long ttl = jwtUtils.remainingTtlSeconds(rawToken);
        if (ttl <= 0) {
            log.debug("Token already expired, no need to blacklist");
            return;
        }

        try {
            redisTemplate.opsForValue().set(
                BLACKLIST_KEY_PREFIX + jti,
                REVOKED_VALUE,
                Duration.ofSeconds(ttl)
            );
            log.debug("Token JTI '{}' blacklisted (TTL: {}s)", jti, ttl);
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis: {}", e.getMessage());
        }
    }

    /**
     * Checks whether a raw token's JTI is present in the blacklist.
     *
     * @param rawToken the raw JWT Bearer token string
     * @return {@code true} if the token has been revoked
     */
    public boolean isBlacklisted(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return false;

        return jwtUtils.extractJti(rawToken)
                .map(jti -> {
                    try {
                        return Boolean.TRUE.equals(
                            redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti)
                        );
                    } catch (Exception e) {
                        // Fail open: if Redis is down, do not block legitimate requests.
                        // Log at ERROR level so the ops team is alerted.
                        log.error("Redis blacklist check failed (fail-open): {}", e.getMessage());
                        return false;
                    }
                })
                .orElse(false);
    }
}
