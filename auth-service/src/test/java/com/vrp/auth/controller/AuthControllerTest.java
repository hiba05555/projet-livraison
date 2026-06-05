package com.vrp.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrp.auth.dto.request.RegisterRequest;
import com.vrp.auth.dto.response.UserResponse;
import com.vrp.auth.entity.UserRole;
import com.vrp.auth.entity.UserStatus;
import com.vrp.auth.service.AuthService;
import com.vrp.auth.service.LogoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean LogoutService logoutService;

    // ── Register ───────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201() throws Exception {
        RegisterRequest req = validRegisterRequest();

        UserResponse response = UserResponse.builder()
                .id("local-id")
                .keycloakUserId("kc-uuid")
                .username("driver1")
                .email("driver1@vrp.com")
                .role(UserRole.DRIVER)
                .status(UserStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(authService.register(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("driver1"));
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setUsername("");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setPassword("password");   // no uppercase, digit, special char

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void logout_authenticated_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer fake.jwt.token")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private RegisterRequest validRegisterRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("driver1");
        r.setEmail("driver1@vrp.com");
        r.setPassword("Driver@2024!");
        r.setFirstName("Jean");
        r.setLastName("Dupont");
        r.setRole(UserRole.DRIVER);
        return r;
    }
}
