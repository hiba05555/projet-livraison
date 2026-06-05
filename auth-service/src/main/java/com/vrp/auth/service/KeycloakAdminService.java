package com.vrp.auth.service;

import com.vrp.auth.audit.AuditAction;
import com.vrp.auth.audit.AuditService;
import com.vrp.auth.client.KeycloakAdminClientWrapper;
import com.vrp.auth.dto.request.AssignRoleRequest;
import com.vrp.auth.dto.request.UpdateProfileRequest;
import com.vrp.auth.dto.response.UserResponse;
import com.vrp.auth.entity.UserProfile;
import com.vrp.auth.entity.UserRole;
import com.vrp.auth.entity.UserStatus;
import com.vrp.auth.mapper.UserMapper;
import com.vrp.auth.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin-level service for user management operations.
 * All methods require ADMIN role (enforced at controller level with {@code @PreAuthorize}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KeycloakAdminService {

    private final KeycloakAdminClientWrapper keycloakClient;
    private final UserProfileService userProfileService;
    private final AuditService auditService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserResponse> listAllUsers() {
        return userMapper.toResponseList(userProfileService.getAll());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String keycloakUserId) {
        UserProfile profile = userProfileService.getByKeycloakId(keycloakUserId);
        return userMapper.toResponse(profile);
    }

    public UserResponse assignRole(String keycloakUserId, AssignRoleRequest request) {
        UserProfile profile = userProfileService.getByKeycloakId(keycloakUserId);
        UserRole oldRole = profile.getRole();
        UserRole newRole = request.getRole();

        // Remove old role in Keycloak and assign new one
        keycloakClient.removeRealmRole(keycloakUserId, oldRole.name());
        keycloakClient.assignRealmRole(keycloakUserId, newRole.name());

        // Update local profile
        userProfileService.updateRole(keycloakUserId, newRole);
        profile.setRole(newRole);

        String adminUser = SecurityUtils.getCurrentUsername().orElse("system");
        auditService.log(
            AuditAction.ROLE_ASSIGNED,
            adminUser,
            keycloakUserId,
            "Role changed: " + oldRole + " → " + newRole
        );

        log.info("Admin '{}' changed role of user '{}': {} → {}", adminUser, keycloakUserId, oldRole, newRole);
        return userMapper.toResponse(profile);
    }

    public UserResponse updateUser(String keycloakUserId, UpdateProfileRequest request) {
        // Update in Keycloak
        keycloakClient.updateUser(keycloakUserId, request);
        // Update local profile
        UserProfile updated = userProfileService.updateProfile(keycloakUserId, request);

        auditService.log(
            AuditAction.USER_PROFILE_UPDATED,
            SecurityUtils.getCurrentUsername().orElse("system"),
            keycloakUserId,
            "Admin profile update"
        );

        return userMapper.toResponse(updated);
    }

    public void suspendUser(String keycloakUserId) {
        keycloakClient.disableUser(keycloakUserId);
        userProfileService.updateStatus(keycloakUserId, UserStatus.SUSPENDED);

        auditService.log(
            AuditAction.USER_SUSPENDED,
            SecurityUtils.getCurrentUsername().orElse("system"),
            keycloakUserId,
            null
        );
    }

    public void activateUser(String keycloakUserId) {
        keycloakClient.enableUser(keycloakUserId);
        userProfileService.updateStatus(keycloakUserId, UserStatus.ACTIVE);

        auditService.log(
            AuditAction.USER_ACTIVATED,
            SecurityUtils.getCurrentUsername().orElse("system"),
            keycloakUserId,
            null
        );
    }

    public void deleteUser(String keycloakUserId) {
        keycloakClient.deleteUser(keycloakUserId);
        userProfileService.deleteProfile(keycloakUserId);

        auditService.log(
            AuditAction.USER_DELETED,
            SecurityUtils.getCurrentUsername().orElse("system"),
            keycloakUserId,
            null
        );
    }
}
