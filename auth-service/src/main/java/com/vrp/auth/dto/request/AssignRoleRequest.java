package com.vrp.auth.dto.request;

import com.vrp.auth.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRoleRequest {

    @NotNull(message = "Role is required")
    private UserRole role;
}
