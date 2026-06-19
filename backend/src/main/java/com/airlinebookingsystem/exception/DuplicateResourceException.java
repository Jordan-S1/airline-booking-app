package com.airlinebookingsystem.exception;

/**
 * Thrown when attempting to create a resource that already exists.
 * Maps to HTTP 409 in GlobalExceptionHandler.
 *
 * Usage: throw new DuplicateResourceException("Email", email);
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resource, Object value) {
        super(resource + " already exists: " + value);
    }
}
