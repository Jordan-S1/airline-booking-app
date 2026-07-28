package com.airlinebookingsystem.dto.flight;

import java.time.LocalDate;
import java.util.List;

/**
 * Results for a multi-city search: one entry per requested leg, in the same
 * order the legs were submitted.
 */
public record MultiCitySearchResult(
        List<LegResult> legs
) {

    public record LegResult(
            int legNumber,
            String departureAirport,
            String arrivalAirport,
            LocalDate departureDate,
            List<FlightSearchResponse> flights
    ) {}
}
