package com.airlinebookingsystem.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response body returned by GlobalExceptionHandler for all
 * errors.
 *
 * Example JSON:
 * {
 * "status": 404,
 * "error": "Not Found",
 * "message": "Flight not found: 99",
 * "timestamp": "2026-06-17T12:00:00",
 * "path": "/api/v1/flights/99",
 * "validationErrors": { "email": "must be a valid email" }
 * }
 *
 * validationErrors is only included when there are @Valid constraint
 * violations.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, String> validationErrors;
}
