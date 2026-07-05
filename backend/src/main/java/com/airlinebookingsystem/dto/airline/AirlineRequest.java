package com.airlinebookingsystem.dto.airline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating an airline.
 * Only exposes fields a client should provide — server manages id, flights, timestamps.
 */
public record AirlineRequest(

        @NotBlank(message = "Airline code is required")
        @Size(min = 2, max = 2, message = "Airline code must be exactly 2 characters (IATA standard)")
        @Pattern(regexp = "^[A-Za-z0-9]{2}$", message = "Airline code must be 2 alphanumeric characters")
        String code,

        @NotBlank(message = "Airline name is required")
        String name,

        String logoUrl,

        String website,

        @NotBlank(message = "Country is required")
        String country,

        Boolean active
) {}

