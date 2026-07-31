package com.airlinebookingsystem.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long flightId,
        String flightNumber,
        String departureAirport,
        String arrivalAirport,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String departureTimezone,
        String arrivalTimezone,
        Integer numberOfPassengers,
        BigDecimal totalAmount,
        String status,
        String seatClass,
        String userEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
