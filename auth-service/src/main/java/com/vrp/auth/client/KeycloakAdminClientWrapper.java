package com.vrp.auth.client;

import com.vrp.auth.dto.request.RegisterRequest;
import com.vrp.auth.dto.request.UpdateProfileRequest;
import com.vrp.auth.exception.KeycloakIntegrationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Low-level wrapper around the Keycloak Admin REST API.
 *
 * <p>This component encapsulates all Keycloak Admin Client calls, providing a
 * typed, exception-safe interface for the service layer. All Keycloak-specific
 * exceptions are translated to {@link KeycloakIntegrationException}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminClientWrapper {

    private final Keycloak keycloakAdmin;

    @Value("${keycloak.realm}")
    private String realm;

    // ── User operations ────────────────────────────────────────────────────

    /**
     * Creates a user in Keycloak and returns their new UUID.
     *
     * @throws KeycloakIntegrationException if creation fails
     */
    public String createUser(RegisterRequest request) {
        UsersResource users = realm().users();

        UserRepresentation user = buildUserRepresentation(request);

        try (Response response = users.create(user)) {
            int status = response.getStatus();
            if (status == 201) {
                String location = response.getHeaderString("Location");
                String userId = location.substring(location.lastIndexOf('/') + 1);
                log.info("Keycloak user created: id={}, username={}", userId, request.getUsername());
                return userId;
            } else if (status == 409) {
                throw new KeycloakIntegrationException("User already exists in Keycloak");
            } else {
                throw new KeycloakIntegrationException(
                    "Keycloak user creation failed with status: " + status);
            }
        } catch (KeycloakIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Keycloak user creation error", ex);
        }
    }

    /**
     * Assigns a realm-level role to a user.
     */
    public void assignRealmRole(String keycloakUserId, String roleName) {
        try {
            RealmResource r = realm();
            List<RoleRepresentation> roles = r.roles().list()
                    .stream()
                    .filter(role -> role.getName().equals(roleName))
                    .toList();

            if (roles.isEmpty()) {
                throw new KeycloakIntegrationException("Role not found in realm: " + roleName);
            }

            r.users().get(keycloakUserId).roles().realmLevel().add(roles);
            log.info("Role '{}' assigned to Keycloak user '{}'", roleName, keycloakUserId);
        } catch (KeycloakIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to assign role '" + roleName + "'", ex);
        }
    }

    /**
     * Removes a realm-level role from a user.
     */
    public void removeRealmRole(String keycloakUserId, String roleName) {
        try {
            RealmResource r = realm();
            List<RoleRepresentation> roles = r.roles().list()
                    .stream()
                    .filter(role -> role.getName().equals(roleName))
                    .toList();

            if (!roles.isEmpty()) {
                r.users().get(keycloakUserId).roles().realmLevel().remove(roles);
                log.info("Role '{}' removed from Keycloak user '{}'", roleName, keycloakUserId);
            }
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to remove role", ex);
        }
    }

    /**
     * Retrieves a Keycloak user by their Keycloak ID.
     */
    public Optional<UserRepresentation> findById(String keycloakUserId) {
        try {
            UserRepresentation user = realm().users().get(keycloakUserId).toRepresentation();
            return Optional.ofNullable(user);
        } catch (Exception ex) {
            log.warn("Keycloak user not found: {}", keycloakUserId);
            return Optional.empty();
        }
    }

    /**
     * Searches Keycloak users by email.
     */
    public List<UserRepresentation> findByEmail(String email) {
        try {
            return realm().users().searchByEmail(email, true);
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to search user by email", ex);
        }
    }

    /**
     * Searches Keycloak users by username.
     */
    public List<UserRepresentation> findByUsername(String username) {
        try {
            return realm().users().searchByUsername(username, true);
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to search user by username", ex);
        }
    }

    /**
     * Sends Keycloak's built-in "UPDATE_PASSWORD" required action to the user's email.
     */
    public void sendPasswordResetEmail(String keycloakUserId) {
        try {
            realm().users().get(keycloakUserId)
                   .executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
            log.info("Password reset email sent to Keycloak user '{}'", keycloakUserId);
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to send password reset email", ex);
        }
    }

    /**
     * Updates first name and last name in Keycloak.
     */
    public void updateUser(String keycloakUserId, UpdateProfileRequest request) {
        try {
            UserResource userResource = realm().users().get(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();

            if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
            if (request.getLastName()  != null) user.setLastName(request.getLastName());

            userResource.update(user);
            log.info("Keycloak user '{}' profile updated", keycloakUserId);
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to update Keycloak user profile", ex);
        }
    }

    /**
     * Disables a Keycloak account (does not delete it).
     */
    public void disableUser(String keycloakUserId) {
        try {
            UserResource userResource = realm().users().get(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(false);
            userResource.update(user);
            log.info("Keycloak user '{}' disabled", keycloakUserId);
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to disable Keycloak user", ex);
        }
    }

    /**
     * Enables a Keycloak account.
     */
    public void enableUser(String keycloakUserId) {
        try {
            UserResource userResource = realm().users().get(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(true);
            userResource.update(user);
            log.info("Keycloak user '{}' enabled", keycloakUserId);
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to enable Keycloak user", ex);
        }
    }

    /**
     * Permanently deletes a Keycloak user. Use with caution.
     */
    public void deleteUser(String keycloakUserId) {
        try {
            try (Response response = realm().users().delete(keycloakUserId)) {
                log.info("Keycloak user '{}' deleted (status: {})", keycloakUserId, response.getStatus());
            }
        } catch (Exception ex) {
            throw new KeycloakIntegrationException("Failed to delete Keycloak user", ex);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private RealmResource realm() {
        return keycloakAdmin.realm(realm);
    }

    private UserRepresentation buildUserRepresentation(RegisterRequest request) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setCredentials(Collections.singletonList(credential));
        return user;
    }
}
