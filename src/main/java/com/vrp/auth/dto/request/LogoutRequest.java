package com.vrp.auth.dto.request;

import lombok.Data;

@Data
public class LogoutRequest {

    /**
     * Optional refresh token. If provided, it will also be blacklisted in Redis
     * so it cannot be used to obtain new access tokens after logout.
     */
    private String refreshToken;
}
