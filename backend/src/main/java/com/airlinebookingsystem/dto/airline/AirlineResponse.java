package com.airlinebookingsystem.dto.airline;

import java.time.LocalDateTime;

/**
 * Response DTO for airline data.
 * Excludes the flights collection to avoid heavy nested payloads.
 * Use GET /api/v1/flights/airline/{code} to retrieve flights for an airline.
 */
public record AirlineResponse(
        Long id,
        String code,
        String name,
        String logoUrl,
        String website,
        String country,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}