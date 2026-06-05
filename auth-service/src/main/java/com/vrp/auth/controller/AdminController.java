package com.vrp.auth.controller;

import com.vrp.auth.dto.request.AssignRoleRequest;
import com.vrp.auth.dto.request.UpdateProfileRequest;
import com.vrp.auth.dto.response.ApiResponse;
import com.vrp.auth.dto.response.UserResponse;
import com.vrp.auth.service.KeycloakAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin REST API — gestion de tous les utilisateurs de la plateforme.
 * Tous les endpoints exigent le rôle ADMIN.
 *
 * Double protection :
 *  - URL-level  : SecurityConfig (/api/v1/admin/**)
 *  - Method-level : @PreAuthorize (défense en profondeur)
 *
 * Pour tester : connectez-vous avec admin-vrp / Admin@VRP2024!
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin – User Management",
     description = "Gestion complète des utilisateurs — ADMIN uniquement. " +
                   "Connectez-vous avec : admin-vrp / Admin@VRP2024!")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final KeycloakAdminService keycloakAdminService;

    // ── GET / — Lister tous les utilisateurs ───────────────────────────────

    @GetMapping
    @Operation(
        summary     = "Lister tous les utilisateurs",
        description = "Retourne tous les profils enregistrés dans PostgreSQL. " +
                      "Inclut les statuts ACTIVE, PENDING, SUSPENDED, INACTIVE."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste retournée"),
        @ApiResponse(responseCode = "401", description = "Token invalide"),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis")
    })
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.ok(keycloakAdminService.listAllUsers()));
    }

    // ── GET /{id} — Détail d'un utilisateur ───────────────────────────────

    @GetMapping("/{keycloakUserId}")
    @Operation(
        summary     = "Obtenir un utilisateur par ID Keycloak",
        description = "L'ID Keycloak est le UUID retourné lors de l'inscription " +
                      "ou visible dans la liste des utilisateurs."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @Parameter(
                description = "UUID Keycloak de l'utilisateur",
                example     = "f3a1b2c3-d4e5-6789-abcd-ef0123456789"
            )
            @PathVariable String keycloakUserId) {

        return ResponseEntity.ok(
            ApiResponse.ok(keycloakAdminService.getUserById(keycloakUserId)));
    }

    // ── PATCH /{id}/role — Changer le rôle ────────────────────────────────

    @PatchMapping("/{keycloakUserId}/role")
    @Operation(
        summary     = "Changer le rôle d'un utilisateur",
        description = "Retire l'ancien rôle et assigne le nouveau dans Keycloak + PostgreSQL. " +
                      "Rôles disponibles : ADMIN, DRIVER, USER."
    )
    @RequestBody(
        required = true,
        content  = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema    = @Schema(implementation = AssignRoleRequest.class),
            examples  = {
                @ExampleObject(
                    name    = "Promouvoir en DRIVER",
                    summary = "Passer un USER en DRIVER",
                    value   = """
                        {
                          "role": "DRIVER"
                        }
                        """
                ),
                @ExampleObject(
                    name    = "Rétrograder en USER",
                    summary = "Retirer les droits avancés",
                    value   = """
                        {
                          "role": "USER"
                        }
                        """
                )
            }
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rôle modifié avec succès"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis"),
        @ApiResponse(responseCode = "502", description = "Erreur Keycloak")
    })
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @Parameter(description = "UUID Keycloak", example = "f3a1b2c3-d4e5-6789-abcd-ef0123456789")
            @PathVariable String keycloakUserId,
            @Valid @org.springframework.web.bind.annotation.RequestBody AssignRoleRequest request) {

        UserResponse updated = keycloakAdminService.assignRole(keycloakUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("Rôle modifié avec succès", updated));
    }

    // ── PUT /{id} — Modifier le profil ────────────────────────────────────

    @PutMapping("/{keycloakUserId}")
    @Operation(
        summary     = "Modifier le profil d'un utilisateur (côté admin)",
        description = "Permet à l'admin de corriger/compléter le profil d'un utilisateur. " +
                      "Synchronisé dans Keycloak ET PostgreSQL."
    )
    @RequestBody(
        required = true,
        content  = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples  = @ExampleObject(
                name  = "Corriger le profil",
                value = """
                    {
                      "firstName":   "Jean",
                      "lastName":    "Dupont",
                      "phoneNumber": "+33612345678"
                    }
                    """
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil mis à jour"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "UUID Keycloak", example = "f3a1b2c3-d4e5-6789-abcd-ef0123456789")
            @PathVariable String keycloakUserId,
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(
            ApiResponse.ok("Profil mis à jour",
                keycloakAdminService.updateUser(keycloakUserId, request)));
    }

    // ── POST /{id}/suspend — Suspendre ────────────────────────────────────

    @PostMapping("/{keycloakUserId}/suspend")
    @Operation(
        summary     = "Suspendre un compte",
        description = "Désactive le compte dans Keycloak (enabled=false) et passe le status en SUSPENDED. " +
                      "L'utilisateur ne peut plus se connecter. " +
                      "Son profil reste en base pour l'historique."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compte suspendu"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis")
    })
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @Parameter(description = "UUID Keycloak", example = "f3a1b2c3-d4e5-6789-abcd-ef0123456789")
            @PathVariable String keycloakUserId) {

        keycloakAdminService.suspendUser(keycloakUserId);
        return ResponseEntity.ok(ApiResponse.ok("Compte suspendu"));
    }

    // ── POST /{id}/activate — Activer ─────────────────────────────────────

    @PostMapping("/{keycloakUserId}/activate")
    @Operation(
        summary     = "Activer un compte",
        description = "Réactive le compte dans Keycloak (enabled=true) et passe le status en ACTIVE. " +
                      "Utilisé pour valider un nouveau compte (PENDING → ACTIVE) " +
                      "ou réactiver un compte suspendu (SUSPENDED → ACTIVE)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compte activé"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis")
    })
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @Parameter(description = "UUID Keycloak", example = "f3a1b2c3-d4e5-6789-abcd-ef0123456789")
            @PathVariable String keycloakUserId) {

        keycloakAdminService.activateUser(keycloakUserId);
        return ResponseEntity.ok(ApiResponse.ok("Compte activé"));
    }

    // ── DELETE /{id} — Supprimer ──────────────────────────────────────────

    @DeleteMapping("/{keycloakUserId}")
    @Operation(
        summary     = "Supprimer définitivement un utilisateur",
        description = "Supprime l'utilisateur dans Keycloak ET dans PostgreSQL. " +
                      "Action irréversible — l'audit_log conserve une trace."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Utilisateur supprimé définitivement"),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis"),
        @ApiResponse(responseCode = "502", description = "Erreur Keycloak")
    })
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "UUID Keycloak", example = "f3a1b2c3-d4e5-6789-abcd-ef0123456789")
            @PathVariable String keycloakUserId) {

        keycloakAdminService.deleteUser(keycloakUserId);
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur supprimé définitivement"));
    }
}
