package com.airlinebookingsystem.exception;

/**
 * Thrown when a requested resource cannot be found.
 * Maps to HTTP 404 in GlobalExceptionHandler.
 *
 * Usage: throw new ResourceNotFoundException("Flight", id);
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " not found: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
