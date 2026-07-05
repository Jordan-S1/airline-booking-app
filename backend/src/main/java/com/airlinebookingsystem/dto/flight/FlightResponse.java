package com.airlinebookingsystem.dto.flight;

import com.airlinebookingsystem.entity.Flight;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for a single flight - returned by POST, PUT, GET by ID/number.
 * Includes full pricing and seat breakdown, excludes bookings collection.
 * For search results use FlightSearchResponse instead.
 */
public record FlightResponse(
        Long id,
        String flightNumber,
        String airlineCode,
        String airlineName,
        String departureAirportCode,
        String departureCity,
        String arrivalAirportCode,
        String arrivalCity,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        Integer duration,
        Integer totalSeats,
        Integer availableSeats,
        Integer economySeats,
        Integer businessSeats,
        Integer firstClassSeats,
        BigDecimal basePrice,
        BigDecimal economyPrice,
        BigDecimal businessPrice,
        BigDecimal firstClassPrice,
        Flight.FlightStatus status,
        Boolean active,
        String aircraft,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
