package com.airlinebookingsystem.dto.flight;

import java.time.LocalDate;

public record FlightSearchRequest(
        String departureAirport,
        String arrivalAirport,
        LocalDate departureDate,
        LocalDate returnDate,
        Integer passengers,
        String seatClass,
        Boolean directFlightsOnly
) {}
