package com.airlinebookingsystem.dto.assistant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

/**
 * What the model claims the traveller asked for.
 *
 * <p><strong>Nothing in here is trusted.</strong> This is the model's reading of
 * a sentence, not a search: every field is checked against the database before
 * any of it reaches {@code FlightService}. Airport codes that do not exist are
 * discarded, dates in the past are refused, passenger counts are clamped, and
 * an unknown cabin falls back to economy. That is why every field is nullable —
 * "the model did not say" and "the model said something unusable" both have to
 * be representable, and neither may become a default that silently searches for
 * something the traveller never asked about.
 *
 * @param clarification a question to put back to the traveller instead of
 *                      searching. The model sets this when the message is
 *                      missing something it cannot invent — most often a
 *                      destination the network does not serve.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedSearch(
        String departureAirport,
        String arrivalAirport,
        LocalDate departureDate,
        LocalDate returnDate,
        Integer passengers,
        String seatClass,
        String clarification
) {}
