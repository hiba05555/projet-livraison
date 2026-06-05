package com.vrp.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class KeycloakIntegrationException extends RuntimeException {
    public KeycloakIntegrationException(String message) { super(message); }
    public KeycloakIntegrationException(String message, Throwable cause) { super(message, cause); }
}
