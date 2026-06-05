package com.vrp.auth.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Keycloak Admin Client singleton.
 *
 * <p>This client authenticates against the {@code master} realm using
 * admin credentials and is used exclusively for realm management operations
 * (user creation, role assignment, etc.).</p>
 *
 * <p>It is <strong>not</strong> used in the end-user authentication flow,
 * which goes directly from the frontend to Keycloak via OIDC.</p>
 */
@Configuration
public class KeycloakAdminConfig {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Bean
    public Keycloak keycloakAdmin() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .grantType("password")
                .username(adminUsername)
                .password(adminPassword)
                .clientId("admin-cli")
                .build();
    }
}
