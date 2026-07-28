package com.airlinebookingsystem.dto.flight;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Live status for a scheduled flight, derived from its timetable.
 *
 * {@code status} and {@code progressPercentage} are computed at request time by
 * comparing "now" against the flight's departure/arrival times — they are not
 * stored. {@code progressPercentage} is 0 before departure and 100 once the
 * flight has arrived.
 */
public record FlightStatusResponse(
        Long id,
        String flightNumber,
        String airlineName,
        String departureAirport,
        String departureCity,
        BigDecimal departureLatitude,
        BigDecimal departureLongitude,
        String arrivalAirport,
        String arrivalCity,
        BigDecimal arrivalLatitude,
        BigDecimal arrivalLongitude,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        Integer duration,
        String status,
        Integer progressPercentage,
        String gate,
        String terminal,
        String aircraft
) {}
