package com.airlinebookingsystem.dto.flight;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A bookable flight. {@code departureTime} and {@code arrivalTime} are UTC
 * instants; render each in the matching IANA timezone so a search result reads
 * in the airport's own local time rather than the viewer's.
 */
public record FlightSearchResponse(
        Long id,
        String flightNumber,
        String airlineName,
        String airlineCode,
        String departureAirport,
        String arrivalAirport,
        String departureCity,
        String arrivalCity,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String departureTimezone,
        String arrivalTimezone,
        Integer duration,
        BigDecimal price,
        Integer availableSeats,
        String aircraft
) {}
