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
 *
 * <p>{@code departureTime} and {@code arrivalTime} are UTC instants, so the
 * arithmetic above works on one timeline. The accompanying IANA timezone ids
 * are what the client renders them in — without those, a Tokyo arrival would
 * display in the viewer's own zone rather than Tokyo's.
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
        String departureTimezone,
        String arrivalTimezone,
        Integer duration,
        String status,
        Integer progressPercentage,
        String gate,
        String terminal,
        String aircraft
) {}
