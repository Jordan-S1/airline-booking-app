package com.airlinebookingsystem.dto.live;

/**
 * A single aircraft currently transmitting ADS-B, sourced from the
 * OpenSky Network. Altitude and speed are converted from the upstream
 * metric units (metres, m/s) to aviation units (feet, knots).
 */
public record LiveFlightResponse(
        String icao24,
        String callsign,
        String originCountry,
        Double latitude,
        Double longitude,
        Integer altitudeFeet,
        Integer speedKnots,
        Integer headingDegrees,
        boolean onGround
) {}
