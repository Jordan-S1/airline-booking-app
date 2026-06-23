package com.airlinebookingsystem.dto.flight;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        Integer duration,
        BigDecimal price,
        Integer availableSeats,
        String aircraft
) {}
