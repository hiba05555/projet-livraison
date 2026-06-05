package com.vrp.auth.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Local mirror of the Keycloak user, enriched with VRP-specific attributes.
 *
 * <p><strong>Source of truth for authentication</strong> is always Keycloak.
 * This entity stores only the data needed by the VRP domain (roles, status, phone)
 * and is kept in sync via the user onboarding and management flows.</p>
 */
@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_user_profiles_keycloak_id", columnList = "keycloak_user_id", unique = true),
    @Index(name = "idx_user_profiles_email",       columnList = "email",             unique = true),
    @Index(name = "idx_user_profiles_username",    columnList = "username",           unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "keycloakUserId")
public class UserProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /** Keycloak subject (sub claim). Primary link to Keycloak. */
    @Column(name = "keycloak_user_id", nullable = false, unique = true)
    private String keycloakUserId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    /** Optional phone number for notifications (SMS / WhatsApp). */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    /** Optimistic locking to prevent concurrent update races. */
    @Version
    @Column(name = "version")
    private Long version;
}
