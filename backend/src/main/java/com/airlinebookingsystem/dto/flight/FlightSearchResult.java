package com.airlinebookingsystem.dto.flight;

import java.util.List;

public record FlightSearchResult(
        List<FlightSearchResponse> outboundFlights,
        List<FlightSearchResponse> returnFlights,
        boolean isRoundTrip
) {}
