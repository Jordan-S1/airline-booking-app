package com.airlinebookingsystem.exception;

/**
 * Thrown when a flight does not have enough seats available in the requested class.
 * Maps to HTTP 409 in GlobalExceptionHandler.
 */
public class InsufficientSeatsException extends RuntimeException {

    public InsufficientSeatsException(String seatClass, int requested, int available) {
        super("Not enough " + seatClass + " seats: requested " + requested + ", available " + available);
    }
}
