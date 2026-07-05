package com.airlinebookingsystem.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a booking.
 *
 * Only booking-level fields can be changed — seat class and flight.
 * Passenger details are managed separately via the passengers endpoint:
 *   POST   /api/v1/passengers/booking/{bookingId}  — add passenger
 *   PUT    /api/v1/passengers/{id}                 — update passenger
 *   DELETE /api/v1/passengers/{id}                 — remove passenger
 */
public record BookingUpdateRequest(
        @NotNull(message = "Flight ID is required")
        Long flightId,

        @NotBlank(message = "Seat class is required")
        String seatClass,

        // Optional — if provided, updates the passenger count for seat reservation.
        // Defaults to current passenger count if not provided.
        Integer numberOfPassengers
) {}