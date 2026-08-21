package com.airlinebookingsystem.dto.assistant;

/**
 * Whether the assistant can be used at all.
 *
 * <p>Polled by the frontend on load so the feature can be hidden when no API
 * key is configured, rather than offered and then failing on first use.
 */
public record AssistantStatusResponse(boolean available) {}
