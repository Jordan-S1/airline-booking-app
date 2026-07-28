package com.airlinebookingsystem.dto.live;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A snapshot of live air traffic over the configured region.
 *
 * {@code retrievedAt} is when the data was actually fetched upstream — it may
 * be older than the request because responses are cached to stay inside
 * OpenSky's free daily credit allowance.
 */
public record LiveTrafficResponse(
        List<LiveFlightResponse> flights,
        int totalTracked,
        String region,
        LocalDateTime retrievedAt
) {}
