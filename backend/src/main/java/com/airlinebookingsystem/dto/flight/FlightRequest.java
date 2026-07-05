package com.airlinebookingsystem.dto.flight;

import com.airlinebookingsystem.entity.Flight;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating or updating a flight.

 * References airline and airports by IATA code - the service resolves
 * them to entities. This avoids the messy nested object structure that
 * comes from accepting the raw Flight entity directly.

 * Example:
 * {
 *   "flightNumber": "EI204",
 *   "airlineCode": "EI",
 *   "departureAirportCode": "DUB",
 *   "arrivalAirportCode": "LHR",
 *   "departureTime": "2026-08-01T06:00:00",
 *   "arrivalTime": "2026-08-01T07:20:00",
 *   "economySeats": 150,
 *   "businessSeats": 30,
 *   "firstClassSeats": 10,
 *   "economyPrice": 89.99,
 *   "businessPrice": 299.99,
 *   "firstClassPrice": 599.99,
 *   "aircraft": "Airbus A320",
 *   "status": "SCHEDULED"
 * }
 */
public record FlightRequest(

        @NotBlank(message = "Flight number is required")
        String flightNumber,

        @NotBlank(message = "Airline code is required")
        String airlineCode,

        @NotBlank(message = "Departure airport code is required")
        String departureAirportCode,

        @NotBlank(message = "Arrival airport code is required")
        String arrivalAirportCode,

        @NotNull(message = "Departure time is required")
        LocalDateTime departureTime,

        @NotNull(message = "Arrival time is required")
        LocalDateTime arrivalTime,

        @NotNull(message = "Economy seats is required")
        @Min(value = 0, message = "Economy seats cannot be negative")
        Integer economySeats,

        @Min(value = 0, message = "Business seats cannot be negative")
        Integer businessSeats,

        @Min(value = 0, message = "First class seats cannot be negative")
        Integer firstClassSeats,

        @NotNull(message = "Economy price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Economy price must be greater than 0")
        BigDecimal economyPrice,

        @DecimalMin(value = "0.0", inclusive = true, message = "Business price cannot be negative")
        BigDecimal businessPrice,

        @DecimalMin(value = "0.0", inclusive = true, message = "First class price cannot be negative")
        BigDecimal firstClassPrice,

        String aircraft,

        Flight.FlightStatus status
) {}
