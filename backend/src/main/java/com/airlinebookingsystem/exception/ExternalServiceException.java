package com.airlinebookingsystem.exception;

/**
 * Thrown when an upstream third-party service (e.g. the OpenSky Network) is
 * unavailable or returns an unusable response.
 * Maps to HTTP 503 in GlobalExceptionHandler.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String service, String reason) {
        super(service + " is currently unavailable: " + reason);
    }
}
