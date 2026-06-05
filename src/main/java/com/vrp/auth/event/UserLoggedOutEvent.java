package com.vrp.auth.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserLoggedOutEvent extends ApplicationEvent {

    private final String keycloakUserId;
    private final String username;
    private final String ipAddress;

    public UserLoggedOutEvent(Object source, String keycloakUserId,
                              String username, String ipAddress) {
        super(source);
        this.keycloakUserId = keycloakUserId;
        this.username = username;
        this.ipAddress = ipAddress;
    }
}
