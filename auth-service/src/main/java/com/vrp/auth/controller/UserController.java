package com.vrp.auth.controller;

import com.vrp.auth.dto.request.UpdateProfileRequest;
import com.vrp.auth.dto.response.ApiResponse;
import com.vrp.auth.dto.response.UserResponse;
import com.vrp.auth.entity.UserProfile;
import com.vrp.auth.mapper.UserMapper;
import com.vrp.auth.service.KeycloakAdminService;
import com.vrp.auth.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Consultation et modification du profil de l'utilisateur connecté")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserProfileService  userProfileService;
    private final KeycloakAdminService keycloakAdminService;
    private final UserMapper          userMapper;

    // ── GET /me ────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(
        summary     = "Consulter mon profil",
        description = "Retourne le profil de l'utilisateur authentifié. " +
                      "L'identité est extraite du JWT (claim 'sub'). " +
                      "Aucun paramètre requis — tout vient du token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil retourné avec succès"),
        @ApiResponse(responseCode = "401", description = "Token manquant, expiré ou blacklisté"),
        @ApiResponse(responseCode = "404", description = "Profil local introuvable (désynchronisation Keycloak)")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {

        UserProfile profile = userProfileService.getByKeycloakId(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.ok(userMapper.toResponse(profile)));
    }

    // ── PUT /me ────────────────────────────────────────────────────────────

    @PutMapping("/me")
    @Operation(
        summary     = "Modifier mon profil",
        description = "Met à jour le prénom, nom et téléphone de l'utilisateur connecté. " +
                      "La modification est synchronisée dans Keycloak ET PostgreSQL. " +
                      "Email et username ne peuvent pas être modifiés ici."
    )
    @RequestBody(
        required = true,
        content  = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema    = @Schema(implementation = UpdateProfileRequest.class),
            examples  = {
                @ExampleObject(
                    name    = "Modifier prénom et nom",
                    summary = "Mise à jour complète du profil",
                    value   = """
                        {
                          "firstName":   "Marie",
                          "lastName":    "Dupont",
                          "phoneNumber": "+33612345678"
                        }
                        """
                ),
                @ExampleObject(
                    name    = "Modifier uniquement le téléphone",
                    summary = "Mise à jour partielle",
                    value   = """
                        {
                          "phoneNumber": "+33698765432"
                        }
                        """
                )
            }
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil mis à jour avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Token manquant ou invalide"),
        @ApiResponse(responseCode = "502", description = "Erreur de synchronisation avec Keycloak")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateProfileRequest request) {

        UserResponse updated = keycloakAdminService.updateUser(jwt.getSubject(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profil mis à jour", updated));
    }
}
