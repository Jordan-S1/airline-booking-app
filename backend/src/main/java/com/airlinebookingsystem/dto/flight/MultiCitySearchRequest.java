package com.airlinebookingsystem.dto.flight;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Search across an arbitrary sequence of legs (e.g. DUB→CDG, CDG→FCO, FCO→DUB).
 * Each leg is searched independently; the caller picks one flight per leg.
 */
public record MultiCitySearchRequest(

        @NotEmpty(message = "At least two legs are required")
        @Size(min = 2, max = 5, message = "A multi-city trip must have between 2 and 5 legs")
        @Valid
        List<Leg> legs,

        Integer passengers,
        String seatClass,
        Boolean directFlightsOnly
) {

    public record Leg(
            @NotNull(message = "Departure airport is required")
            String departureAirport,

            @NotNull(message = "Arrival airport is required")
            String arrivalAirport,

            @NotNull(message = "Departure date is required")
            LocalDate departureDate
    ) {}
}
