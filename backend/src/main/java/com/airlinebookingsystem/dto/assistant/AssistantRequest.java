package com.airlinebookingsystem.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A natural-language request to the travel assistant.
 *
 * @param message    what the traveller typed, e.g. "cheapest flight to Paris
 *                   next Friday"
 * @param originHint the IATA code of the airport the traveller is browsing
 *                   from, used only when the message itself names no origin.
 *                   Optional, and validated against the database like any
 *                   other code — a caller cannot use it to search an airport
 *                   the network does not serve.
 */
public record AssistantRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must be 500 characters or fewer")
        String message,

        String originHint
) {}
