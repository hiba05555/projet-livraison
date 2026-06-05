package com.vrp.auth.controller;

import com.vrp.auth.dto.request.LogoutRequest;
import com.vrp.auth.dto.request.PasswordResetRequest;
import com.vrp.auth.dto.request.RegisterRequest;
import com.vrp.auth.dto.response.ApiResponse;
import com.vrp.auth.dto.response.UserResponse;
import com.vrp.auth.entity.UserRole;
import com.vrp.auth.entity.UserStatus;
import com.vrp.auth.repository.UserProfileRepository;
import com.vrp.auth.service.AuthService;
import com.vrp.auth.service.LogoutService;
import com.vrp.auth.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse  ;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Inscription, déconnexion et gestion des mots de passe. " +
                                            "Le login est géré directement par Keycloak (PKCE).")
public class AuthController {

    private final AuthService            authService;
    private final LogoutService          logoutService;
    private final UserProfileRepository  userProfileRepository;

    // ── Inscription ────────────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(
        summary     = "Créer un compte utilisateur",
        description = "Crée l'utilisateur dans Keycloak via Admin API et un profil local en base. " +
                      "Rôles autorisés en self-service : USER et DRIVER uniquement. " +
                      "ADMIN est bloqué et forcé à USER."
    )
    @RequestBody(
        required = true,
        content  = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema    = @Schema(implementation = RegisterRequest.class),
            examples  = {
                @ExampleObject(
                    name    = "Inscription CLIENT (épicier)",
                    summary = "Créer un compte client",
                    value   = """
                        {
                          "username":    "epicier_test",
                          "email":       "epicier@vrp.com",
                          "password":    "Test@1234!",
                          "firstName":   "Marie",
                          "lastName":    "Martin",
                          "phoneNumber": "+33612345678",
                          "role":        "USER"
                        }
                        """
                ),
                @ExampleObject(
                    name    = "Inscription CHAUFFEUR",
                    summary = "Créer un compte chauffeur",
                    value   = """
                        {
                          "username":    "chauffeur_test",
                          "email":       "chauffeur@vrp.com",
                          "password":    "Test@1234!",
                          "firstName":   "Jean",
                          "lastName":    "Dupont",
                          "phoneNumber": "+33698765432",
                          "role":        "DRIVER"
                        }
                        """
                ),
                @ExampleObject(
                    name    = "Tentative ADMIN (bloquée → forcé USER)",
                    summary = "Rôle ADMIN bloqué automatiquement",
                    value   = """
                        {
                          "username":  "hacker_test",
                          "email":     "hacker@vrp.com",
                          "password":  "Test@1234!",
                          "firstName": "Bad",
                          "lastName":  "Actor",
                          "role":      "ADMIN"
                        }
                        """
                )
            }
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides (email, password, username)"),
        @ApiResponse(responseCode = "409", description = "Email ou username déjà utilisé")
    })
    public ResponseEntity<com.vrp.auth.dto.response.ApiResponse<UserResponse>> register(
            @Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ip = SecurityUtils.extractIpAddress(httpRequest);
        UserResponse user = authService.register(request, ip);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(com.vrp.auth.dto.response.ApiResponse.ok("Compte créé avec succès", user));
    }

    // ── Déconnexion ────────────────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(
        summary     = "Déconnexion sécurisée",
        description = "Blackliste le JTI du token dans Redis avec un TTL = durée restante du token. " +
                      "Toute requête suivante avec ce token sera rejetée avec 401. " +
                      "Le refresh_token peut aussi être blacklisté si fourni.",
        security    = @SecurityRequirement(name = "BearerAuth")
    )
    @RequestBody(
        required = false,
        content  = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples  = {
                @ExampleObject(
                    name    = "Logout avec refresh token",
                    summary = "Blackliste access + refresh token",
                    value   = """
                        {
                          "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
                        }
                        """
                ),
                @ExampleObject(
                    name    = "Logout sans refresh token",
                    summary = "Blackliste uniquement l'access token",
                    value   = "{}"
                )
            }
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Déconnecté avec succès"),
        @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
    })
    public ResponseEntity<com.vrp.auth.dto.response.ApiResponse<Void>> logout(
            @AuthenticationPrincipal Jwt jwt,
            @org.springframework.web.bind.annotation.RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {

        String accessToken  = extractBearerToken(httpRequest);
        String refreshToken = request != null ? request.getRefreshToken() : null;
        String ip           = SecurityUtils.extractIpAddress(httpRequest);

        logoutService.logout(jwt, accessToken, refreshToken, ip);
        return ResponseEntity.ok(com.vrp.auth.dto.response.ApiResponse.ok("Déconnecté avec succès"));
    }

    // ── Réinitialisation du mot de passe ───────────────────────────────────

    @PostMapping("/password-reset")
    @Operation(
        summary     = "Demande de réinitialisation de mot de passe",
        description = "Déclenche l'action UPDATE_PASSWORD de Keycloak. " +
                      "Un email est envoyé si l'adresse existe. " +
                      "Retourne toujours 200 pour éviter l'énumération d'utilisateurs."
    )
    @RequestBody(
        required = true,
        content  = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples  = @ExampleObject(
                name  = "Reset par email",
                value = """
                    {
                      "email": "driver1@vrp.com"
                    }
                    """
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email envoyé si l'adresse existe")
    })
    public ResponseEntity<com.vrp.auth.dto.response.ApiResponse<Void>> requestPasswordReset(
            @Valid @org.springframework.web.bind.annotation.RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        authService.requestPasswordReset(
            request, SecurityUtils.extractIpAddress(httpRequest));
        return ResponseEntity.ok(com.vrp.auth.dto.response.ApiResponse.ok(
            "Si l'email existe, un lien de réinitialisation a été envoyé."));
    }

    // ── Chauffeurs disponibles (endpoint interne) ──────────────────────────

    @GetMapping("/chauffeurs/disponibles")
    @Operation(
        summary     = "Liste des chauffeurs disponibles",
        description = "Endpoint interne appelé par le VRP Service via Docker DNS. " +
                      "Retourne les profils avec role=DRIVER et status=ACTIVE. " +
                      "Aucun JWT requis — accessible uniquement depuis le réseau Docker interne.",
        tags        = {"Internal"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
    })
    public ResponseEntity<com.vrp.auth.dto.response.ApiResponse<List<Map<String, String>>>> getChauffeursDisponibles() {

        List<Map<String, String>> chauffeurs = userProfileRepository
                .findByRoleAndStatus(UserRole.DRIVER, UserStatus.ACTIVE)
                .stream()
                .map(p -> Map.of(
                        "keycloakUserId", p.getKeycloakUserId(),
                        "username",       p.getUsername(),
                        "email",          p.getEmail(),
                        "nom",            p.getLastName()  != null ? p.getLastName()  : "",
                        "prenom",         p.getFirstName() != null ? p.getFirstName() : ""
                ))
                .toList();

        return ResponseEntity.ok(com.vrp.auth.dto.response.ApiResponse.ok(
            chauffeurs.size() + " chauffeur(s) disponible(s)", chauffeurs));
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer "))
               ? header.substring(7) : null;
    }
}
