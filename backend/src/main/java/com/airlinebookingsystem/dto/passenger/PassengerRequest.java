package com.airlinebookingsystem.dto.passenger;

import java.time.LocalDate;

public record PassengerRequest(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String passportNumber,
        String nationality,
        String passengerType
) {}
