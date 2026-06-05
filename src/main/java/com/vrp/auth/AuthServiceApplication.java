package com.vrp.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * VRP Auth Service – Entry point.
 *
 * <p>This microservice acts as the <strong>authentication orchestration layer</strong>
 * for the VRP platform. It does NOT implement any cryptographic primitives or
 * custom OAuth2 server logic – all authentication and token issuance is delegated
 * to <strong>Keycloak</strong>.</p>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>User registration &amp; onboarding (creates users in Keycloak + local profile)</li>
 *   <li>Role management (ADMIN / DRIVER / USER)</li>
 *   <li>Stateless JWT validation via Spring OAuth2 Resource Server (RS256 + JWKS)</li>
 *   <li>Secure logout via Redis token blacklist</li>
 *   <li>Audit logging of all auth events</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
