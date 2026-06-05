package com.vrp.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;

    /** Field-level validation errors: { "fieldName": "error message" } */
    private final Map<String, String> fieldErrors;

    @Builder.Default
    private final Instant timestamp = Instant.now();
}
