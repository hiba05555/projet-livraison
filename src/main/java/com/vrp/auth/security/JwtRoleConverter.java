package com.vrp.auth.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts Keycloak realm roles to Spring Security {@code ROLE_} prefixed authorities.
 *
 * <p>Keycloak JWT payload structure:
 * <pre>{@code
 * {
 *   "realm_access": {
 *     "roles": ["ADMIN", "DRIVER", "default-roles-vrp", ...]
 *   }
 * }
 * }</pre>
 * </p>
 *
 * <p>Only VRP domain roles are mapped; Keycloak internal roles
 * (e.g., {@code default-roles-vrp}, {@code offline_access}) are filtered out.</p>
 */
@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String[] VRP_ROLES = {"ADMIN", "DISPATCHER", "DRIVER", "USER"};

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(SecurityConstants.CLAIM_REALM_ACCESS);
        if (realmAccess == null || !realmAccess.containsKey(SecurityConstants.CLAIM_ROLES)) {
            return Collections.emptyList();
        }

        List<String> keycloakRoles = (List<String>) realmAccess.get(SecurityConstants.CLAIM_ROLES);

        return keycloakRoles.stream()
                .filter(this::isVrpRole)
                .map(role -> new SimpleGrantedAuthority(SecurityConstants.SPRING_ROLE_PREFIX + role))
                .collect(Collectors.toList());
    }

    private boolean isVrpRole(String role) {
        for (String vrpRole : VRP_ROLES) {
            if (vrpRole.equals(role)) return true;
        }
        return false;
    }
}
