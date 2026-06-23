package com.airlinebookingsystem.dto.passenger;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PassengerResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String passportNumber,
        String nationality,
        String seatNumber,
        String passengerType,
        Long bookingId,
        String bookingReference,
        String flightNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
