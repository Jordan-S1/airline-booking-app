package com.airlinebookingsystem.dto.assistant;

import com.airlinebookingsystem.dto.flight.FlightSearchRequest;
import com.airlinebookingsystem.dto.flight.FlightSearchResponse;

import java.util.List;

/**
 * The assistant's answer.
 *
 * @param flights      the rows {@code FlightService.searchFlights} returned, and
 *                     only those. The model never contributes a flight to this
 *                     list — it writes {@link #reply} about the list, which is a
 *                     different job. On a round trip this holds the outbound
 *                     leg; {@link #interpretedAs} carries the return date so the
 *                     full search can be reproduced on the search page.
 * @param interpretedAs the validated search that was actually run, so the
 *                     traveller can see how their sentence was read and the
 *                     frontend can hand off to the normal search UI. Null when
 *                     no search took place.
 * @param needsMoreInfo true when nothing was searched because something had to
 *                     be asked first. {@link #reply} is then the question.
 */
public record AssistantResponse(
        String reply,
        List<FlightSearchResponse> flights,
        FlightSearchRequest interpretedAs,
        boolean needsMoreInfo
) {

    /** An answer that asks a question instead of searching. */
    public static AssistantResponse question(String reply) {
        return new AssistantResponse(reply, List.of(), null, true);
    }
}
