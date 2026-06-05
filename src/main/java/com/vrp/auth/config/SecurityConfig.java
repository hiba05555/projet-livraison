package com.vrp.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrp.auth.redis.TokenBlacklistService;
import com.vrp.auth.security.JwtRoleConverter;
import com.vrp.auth.security.SecurityConstants;
import com.vrp.auth.security.TokenBlacklistFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 6 configuration.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>Stateless sessions – no server-side session state</li>
 *   <li>CSRF disabled – safe for stateless JWT Bearer APIs</li>
 *   <li>OAuth2 Resource Server – validates JWT via Keycloak JWKS (RS256)</li>
 *   <li>Token blacklist filter – rejects logged-out tokens before JWT validation</li>
 *   <li>Method security enabled – supports {@code @PreAuthorize} on service methods</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRoleConverter jwtRoleConverter;
    private final TokenBlacklistService blacklistService;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── Session & CSRF ─────────────────────────────────────────
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Authorization rules ────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public – no token required
                .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                // Admin-only endpoints
                .requestMatchers("/api/v1/admin/**").hasRole(SecurityConstants.ROLE_ADMIN)
                // Dispatcher and above can manage routes
                .requestMatchers(HttpMethod.GET, "/api/v1/users/**").authenticated()
                // Everything else – must be authenticated
                .anyRequest().authenticated()
            )

            // ── OAuth2 Resource Server (JWT validation via JWKS) ───────
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )

            // ── Token blacklist filter ─────────────────────────────────
            // Runs BEFORE Spring Security processes the JWT.
            // If the JTI is found in Redis blacklist → reject 401 immediately.
            .addFilterBefore(
                new TokenBlacklistFilter(blacklistService, objectMapper),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtRoleConverter);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
