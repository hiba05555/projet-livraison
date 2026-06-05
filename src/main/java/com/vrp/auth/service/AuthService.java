package com.vrp.auth.service;

import com.vrp.auth.audit.AuditAction;
import com.vrp.auth.audit.AuditService;
import com.vrp.auth.client.KeycloakAdminClientWrapper;
import com.vrp.auth.dto.request.PasswordResetRequest;
import com.vrp.auth.dto.request.RegisterRequest;
import com.vrp.auth.dto.response.UserResponse;
import com.vrp.auth.entity.UserProfile;
import com.vrp.auth.entity.UserRole;
import com.vrp.auth.event.UserRegisteredEvent;
import com.vrp.auth.exception.UserAlreadyExistsException;
import com.vrp.auth.mapper.UserMapper;
import com.vrp.auth.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the user registration and onboarding flow.
 *
 * <p>Registration sequence:
 * <ol>
 *   <li>Validate uniqueness (email, username) in local DB</li>
 *   <li>Enforce role restriction (only ADMIN can grant the ADMIN role)</li>
 *   <li>Create user in Keycloak via Admin API</li>
 *   <li>Assign realm role in Keycloak</li>
 *   <li>Create local {@link UserProfile} in PostgreSQL</li>
 *   <li>Publish {@link UserRegisteredEvent} for downstream listeners</li>
 *   <li>Write async audit log</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final KeycloakAdminClientWrapper keycloakClient;
    private final UserProfileService userProfileService;
    private final UserProfileRepository userProfileRepository;
    private final AuditService auditService;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Registers a new user and returns their profile.
     *
     * @param request   validated registration data
     * @param ipAddress client IP for audit trail
     * @return the created {@link UserResponse}
     */
    public UserResponse register(RegisterRequest request, String ipAddress) {
        // ── 1. Check local uniqueness ──────────────────────────────────────
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }
        if (userProfileRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }

        // ── 2. Restrict sensitive role assignment ──────────────────────────
        // Self-service registration allows only USER and DRIVER.
        // ADMIN must be assigned via the admin API.
        UserRole requestedRole = request.getRole();
        if (requestedRole == UserRole.ADMIN) {
            log.warn("Attempt to self-register with role '{}' blocked. Defaulting to USER.", requestedRole);
            request.setRole(UserRole.USER);
        }

        // ── 3. Create user in Keycloak ─────────────────────────────────────
        String keycloakUserId = keycloakClient.createUser(request);

        // ── 4. Assign realm role in Keycloak ───────────────────────────────
        keycloakClient.assignRealmRole(keycloakUserId, request.getRole().name());

        // ── 5. Create local profile ────────────────────────────────────────
        UserProfile profile = userProfileService.createProfile(request, keycloakUserId);

        // ── 6. Publish event (async notification, welcome email, etc.) ─────
        eventPublisher.publishEvent(new UserRegisteredEvent(
            this, keycloakUserId, request.getUsername(),
            request.getEmail(), request.getRole(), ipAddress
        ));

        // ── 7. Audit log (async, new transaction) ──────────────────────────
        auditService.log(
            AuditAction.USER_REGISTERED,
            request.getUsername(),
            keycloakUserId,
            ipAddress,
            "Role: " + request.getRole()
        );

        log.info("User '{}' registered successfully with role '{}'",
                 request.getUsername(), request.getRole());

        return userMapper.toResponse(profile);
    }

    /**
     * Triggers Keycloak's built-in password-reset email flow.
     */
    public void requestPasswordReset(PasswordResetRequest request, String ipAddress) {
        userProfileRepository.findByEmail(request.getEmail())
                .ifPresent(profile -> {
                    keycloakClient.sendPasswordResetEmail(profile.getKeycloakUserId());
                    auditService.log(
                        AuditAction.PASSWORD_RESET_REQUESTED,
                        profile.getUsername(),
                        profile.getKeycloakUserId(),
                        ipAddress,
                        null
                    );
                });
        // Always return 200 to avoid user enumeration attacks
    }
}
