package com.vrp.auth.entity;

/**
 * Rôles de la plateforme VRP alignés avec les rôles realm Keycloak.
 *
 * <ul>
 *   <li>ADMIN  – accès complet (admin entreprise)</li>
 *   <li>DRIVER – chauffeur livreur</li>
 *   <li>USER   – client épicier</li>
 * </ul>
 */
public enum UserRole {
    ADMIN,
    DRIVER,
    USER
}
