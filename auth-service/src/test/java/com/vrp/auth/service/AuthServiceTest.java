package com.vrp.auth.service;

import com.vrp.auth.audit.AuditService;
import com.vrp.auth.client.KeycloakAdminClientWrapper;
import com.vrp.auth.dto.request.RegisterRequest;
import com.vrp.auth.dto.response.UserResponse;
import com.vrp.auth.entity.UserProfile;
import com.vrp.auth.entity.UserRole;
import com.vrp.auth.entity.UserStatus;
import com.vrp.auth.exception.UserAlreadyExistsException;
import com.vrp.auth.mapper.UserMapper;
import com.vrp.auth.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock KeycloakAdminClientWrapper keycloakClient;
    @Mock UserProfileService userProfileService;
    @Mock UserProfileRepository userProfileRepository;
    @Mock AuditService auditService;
    @Mock UserMapper userMapper;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AuthService authService;

    RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("testuser");
        validRequest.setEmail("test@vrp.com");
        validRequest.setPassword("Test@2024!");
        validRequest.setFirstName("Test");
        validRequest.setLastName("User");
        validRequest.setRole(UserRole.DRIVER);
    }

    @Test
    void register_newUser_success() {
        UserProfile profile = UserProfile.builder()
                .id("local-id")
                .keycloakUserId("kc-uuid")
                .username("testuser")
                .email("test@vrp.com")
                .role(UserRole.DRIVER)
                .status(UserStatus.PENDING)
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .id("local-id")
                .username("testuser")
                .role(UserRole.DRIVER)
                .build();

        when(userProfileRepository.existsByEmail(anyString())).thenReturn(false);
        when(userProfileRepository.existsByUsername(anyString())).thenReturn(false);
        when(keycloakClient.createUser(any())).thenReturn("kc-uuid");
        when(userProfileService.createProfile(any(), anyString())).thenReturn(profile);
        when(userMapper.toResponse(any())).thenReturn(expectedResponse);

        UserResponse result = authService.register(validRequest, "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");

        verify(keycloakClient).createUser(validRequest);
        verify(keycloakClient).assignRealmRole("kc-uuid", "DRIVER");
        verify(userProfileService).createProfile(validRequest, "kc-uuid");
        verify(auditService).log(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void register_duplicateEmail_throwsUserAlreadyExistsException() {
        when(userProfileRepository.existsByEmail("test@vrp.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest, "127.0.0.1"))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verifyNoInteractions(keycloakClient);
    }

    @Test
    void register_adminRoleRequest_downgradestoUser() {
        validRequest.setRole(UserRole.ADMIN);   // attempt to self-register as ADMIN

        when(userProfileRepository.existsByEmail(anyString())).thenReturn(false);
        when(userProfileRepository.existsByUsername(anyString())).thenReturn(false);
        when(keycloakClient.createUser(any())).thenReturn("kc-uuid");
        when(userProfileService.createProfile(any(), anyString())).thenReturn(
            UserProfile.builder().role(UserRole.USER).build()
        );
        when(userMapper.toResponse(any())).thenReturn(
            UserResponse.builder().role(UserRole.USER).build()
        );

        authService.register(validRequest, "127.0.0.1");

        // Should have been downgraded to USER
        assertThat(validRequest.getRole()).isEqualTo(UserRole.USER);
    }
}
