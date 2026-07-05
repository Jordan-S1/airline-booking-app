package com.airlinebookingsystem.dto.airport;

import java.time.LocalDateTime;

/**
 * Response DTO for airport data.
 * Excludes departure/arrival flight collections to avoid heavy nested payloads.
 * Use GET /api/v1/flights/search to find flights between airports.
 */
public record AirportResponse(
        Long id,
        String code,
        String name,
        String city,
        String country,
        String timezone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
