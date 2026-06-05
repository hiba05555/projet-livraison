package com.vrp.auth.event;

import com.vrp.auth.entity.UserRole;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserRegisteredEvent extends ApplicationEvent {

    private final String keycloakUserId;
    private final String username;
    private final String email;
    private final UserRole role;
    private final String ipAddress;

    public UserRegisteredEvent(Object source, String keycloakUserId,
                               String username, String email,
                               UserRole role, String ipAddress) {
        super(source);
        this.keycloakUserId = keycloakUserId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.ipAddress = ipAddress;
    }
}
