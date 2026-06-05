package com.vrp.auth.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI 3.0 — Swagger UI disponible sur :
 * http://localhost:8081/swagger-ui.html
 *
 * Deux méthodes d'authentification dans Swagger :
 *  1. BearerAuth  → coller un JWT Keycloak directement
 *  2. Keycloak    → flow Authorization Code PKCE complet
 *
 * Comptes de test :
 *  ADMIN      : admin-vrp / Admin@VRP2024!
 *  DISPATCHER : dispatcher1 / Dispatcher@2024!
 *  DRIVER     : driver1 / Driver@2024!
 *  USER       : client1 / Client@2024!
 */
@SecurityScheme(
    name         = "BearerAuth",
    type         = SecuritySchemeType.HTTP,
    scheme       = "bearer",
    bearerFormat = "JWT",
    description  = "Collez votre access_token Keycloak. " +
                   "Obtenez-le via : POST http://localhost:8090/realms/vrp/protocol/openid-connect/token"
)
@Configuration
public class OpenApiConfig {

    @Value("${keycloak.server-url:http://localhost:8090}")
    private String keycloakUrl;

    @Value("${keycloak.realm:vrp}")
    private String realm;

    @Bean
    public OpenAPI openAPI() {
        String authUrl  = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/auth";
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        return new OpenAPI()
            .info(new Info()
                .title("VRP Auth Service API")
                .version("1.0.0")
                .description("""
                    ## Auth Service — VRP Platform

                    Microservice d'authentification et de gestion des utilisateurs.

                    ### Flux d'authentification (PKCE)
                    Le login se fait directement entre le SPA et Keycloak.
                    Ce service gère : inscription, déconnexion, profil, administration.

                    ### Comment tester avec Swagger

                    **Option 1 — Bearer Token (plus simple)**
                    1. Obtenez un token via curl :
                    ```
                    curl -X POST http://localhost:8090/realms/vrp/protocol/openid-connect/token \\
                      -d "grant_type=password&client_id=auth-service" \\
                      -d "client_secret=auth-service-secret-change-in-prod" \\
                      -d "username=admin-vrp&password=Admin@VRP2024!"
                    ```
                    2. Copiez l'access_token
                    3. Cliquez sur **Authorize** → collez le token

                    **Option 2 — OAuth2 PKCE via Swagger**
                    Cliquez sur **Authorize** → section Keycloak → Login

                    ### Comptes de test
                    | Username | Password | Rôle |
                    |---|---|---|
                    | admin-vrp | Admin@VRP2024! | ADMIN |
                    | dispatcher1 | Dispatcher@2024! | DISPATCHER |
                    | driver1 | Driver@2024! | DRIVER |
                    | client1 | Client@2024! | USER |
                    """)
                .contact(new Contact()
                    .name("Équipe VRP — Mariem")
                    .email("mariem@vrp.com"))
            )
            .servers(List.of(
                new Server().url("http://localhost:8081").description("Développement local"),
                new Server().url("http://localhost:8080").description("Via API Gateway")
            ))
            .tags(List.of(
                new Tag().name("Authentication")
                         .description("Inscription, déconnexion, réinitialisation mot de passe"),
                new Tag().name("User Profile")
                         .description("Consultation et modification du profil connecté"),
                new Tag().name("Admin – User Management")
                         .description("Gestion des utilisateurs — ADMIN uniquement"),
                new Tag().name("Internal")
                         .description("Endpoints internes — appelés par d'autres microservices")
            ))
            .components(new Components()
                .addSecuritySchemes("BearerAuth",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Collez votre access_token Keycloak")
                )
                .addSecuritySchemes("Keycloak",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(Type.OAUTH2)
                        .description("Authorization Code Flow + PKCE via Keycloak")
                        .flows(new OAuthFlows()
                            .authorizationCode(new OAuthFlow()
                                .authorizationUrl(authUrl)
                                .tokenUrl(tokenUrl)
                            )
                        )
                )
            );
    }
}
