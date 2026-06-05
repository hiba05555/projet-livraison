package com.vrp.auth.repository;

import com.vrp.auth.entity.UserProfile;
import com.vrp.auth.entity.UserRole;
import com.vrp.auth.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    Optional<UserProfile> findByKeycloakUserId(String keycloakUserId);

    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByKeycloakUserId(String keycloakUserId);

    List<UserProfile> findAllByRole(UserRole role);

    List<UserProfile> findAllByStatus(UserStatus status);

    // Utilisé par GET /api/v1/auth/chauffeurs/disponibles (appelé par vrp-service)
    List<UserProfile> findByRoleAndStatus(UserRole role, UserStatus status);

    @Modifying
    @Query("UPDATE UserProfile u SET u.status = :status WHERE u.keycloakUserId = :keycloakUserId")
    void updateStatusByKeycloakUserId(String keycloakUserId, UserStatus status);

    @Modifying
    @Query("UPDATE UserProfile u SET u.role = :role WHERE u.keycloakUserId = :keycloakUserId")
    void updateRoleByKeycloakUserId(String keycloakUserId, UserRole role);
}
