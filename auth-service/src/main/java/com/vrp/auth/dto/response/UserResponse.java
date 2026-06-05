package com.vrp.auth.dto.response;

import com.vrp.auth.entity.UserRole;
import com.vrp.auth.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserResponse {
    private String id;
    private String keycloakUserId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
