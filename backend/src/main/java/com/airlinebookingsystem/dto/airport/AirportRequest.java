package com.airlinebookingsystem.dto.airport;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating an airport.
 * Only exposes fields a client should provide — server manages id, flights, timestamps.
 */
public record AirportRequest(

        @NotBlank(message = "Airport code is required")
        @Size(min = 3, max = 3, message = "Airport code must be exactly 3 characters (IATA standard)")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Airport code must be 3 letters")
        String code,

        @NotBlank(message = "Airport name is required")
        String name,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Country is required")
        String country,

        @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country code must be 2 letters (ISO 3166-1 alpha-2)")
        String countryCode,

        String timezone,

        /*
         * The airport reference point. Optional, but an airport without one is
         * invisible on the route map — RouteMap has no position to project.
         * Coordinates were added to the table in V6 and this DTO was never
         * updated to carry them, so anything created through the API landed
         * without a position however complete the caller's payload was.
         */
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        BigDecimal longitude
) {}
