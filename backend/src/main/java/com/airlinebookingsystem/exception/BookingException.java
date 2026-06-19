package com.airlinebookingsystem.exception;

/**
 * Thrown for invalid booking state transitions or business rule violations.
 * Maps to HTTP 400 in GlobalExceptionHandler.
 *
 * Examples: confirming a cancelled booking, updating a confirmed booking.
 */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
