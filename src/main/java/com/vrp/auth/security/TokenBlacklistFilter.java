package com.vrp.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrp.auth.dto.response.ErrorResponse;
import com.vrp.auth.redis.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that rejects requests carrying a blacklisted JWT.
 *
 * <p>This filter runs <strong>before</strong> Spring Security's OAuth2 Resource Server
 * JWT validation. When a token is found in the Redis blacklist (i.e., the user has
 * logged out), the request is immediately rejected with {@code 401 Unauthorized}.</p>
 *
 * <p>Filter order: {@code TokenBlacklistFilter} → OAuth2 JWT Validation → Controllers</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistFilter extends OncePerRequestFilter {

    private final TokenBlacklistService blacklistService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token != null) {
            try {
                if (blacklistService.isBlacklisted(token)) {
                    log.warn("Rejected blacklisted token from IP: {}",
                             request.getRemoteAddr());
                    rejectWithUnauthorized(response, "Token has been revoked – please log in again");
                    return;
                }
            } catch (Exception e) {
                // Fail open: let Spring Security handle any unexpected errors
                log.error("Blacklist filter encountered error: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    private void rejectWithUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(message)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
