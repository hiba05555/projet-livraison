package com.vrp.auth.service;

import com.vrp.auth.dto.request.UpdateProfileRequest;
import com.vrp.auth.dto.request.RegisterRequest;
import com.vrp.auth.entity.UserProfile;
import com.vrp.auth.entity.UserRole;
import com.vrp.auth.entity.UserStatus;
import com.vrp.auth.exception.UserNotFoundException;
import com.vrp.auth.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the local {@link UserProfile} – the VRP-domain mirror of Keycloak users.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    /**
     * Creates the local profile after successful Keycloak user creation.
     */
    public UserProfile createProfile(RegisterRequest request, String keycloakUserId) {
        UserProfile profile = UserProfile.builder()
                .keycloakUserId(keycloakUserId)
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .status(UserStatus.PENDING)
                .build();

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Local profile created for user '{}' (keycloakId: {})",
                 request.getUsername(), keycloakUserId);
        return saved;
    }

    @Transactional(readOnly = true)
    public UserProfile getByKeycloakId(String keycloakUserId) {
        return userProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new UserNotFoundException(keycloakUserId));
    }

    @Transactional(readOnly = true)
    public List<UserProfile> getAll() {
        return userProfileRepository.findAll();
    }

    public UserProfile updateProfile(String keycloakUserId, UpdateProfileRequest request) {
        UserProfile profile = getByKeycloakId(keycloakUserId);

        if (request.getFirstName()   != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName()    != null) profile.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) profile.setPhoneNumber(request.getPhoneNumber());

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Profile updated for user '{}'", keycloakUserId);
        return saved;
    }

    public void updateRole(String keycloakUserId, UserRole role) {
        userProfileRepository.updateRoleByKeycloakUserId(keycloakUserId, role);
        log.info("Role updated to '{}' for user '{}'", role, keycloakUserId);
    }

    public void updateStatus(String keycloakUserId, UserStatus status) {
        userProfileRepository.updateStatusByKeycloakUserId(keycloakUserId, status);
        log.info("Status updated to '{}' for user '{}'", status, keycloakUserId);
    }

    public void activateProfile(String keycloakUserId) {
        updateStatus(keycloakUserId, UserStatus.ACTIVE);
    }

    public void deleteProfile(String keycloakUserId) {
        userProfileRepository.findByKeycloakUserId(keycloakUserId)
                .ifPresent(p -> {
                    userProfileRepository.delete(p);
                    log.info("Local profile deleted for user '{}'", keycloakUserId);
                });
    }
}
