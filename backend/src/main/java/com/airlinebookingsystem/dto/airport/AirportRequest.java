package com.airlinebookingsystem.dto.airport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

        String timezone
) {}
