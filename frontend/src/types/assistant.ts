import type { FlightSearchRequestDto, FlightSearchResponseDto } from "./flight";

/** Mirrors com.airlinebookingsystem.dto.assistant.AssistantRequest. */
export interface AssistantRequestDto {
  message: string;
  /**
   * The airport the traveller is browsing from, used only when their message
   * names no origin. Validated server-side like any other code, so sending a
   * stale or wrong one cannot search an airport the network does not serve.
   */
  originHint: string | null;
}

/**
 * Mirrors com.airlinebookingsystem.dto.assistant.AssistantResponse.
 *
 * `flights` is whatever the database returned - the model writes `reply` about
 * those rows but never contributes one. Render the rows, not the prose, when
 * the two disagree.
 */
export interface AssistantResponseDto {
  reply: string;
  flights: FlightSearchResponseDto[];
  /** The validated search that ran, or null when a question was asked instead. */
  interpretedAs: FlightSearchRequestDto | null;
  /** True when nothing was searched because something had to be asked first. */
  needsMoreInfo: boolean;
}

/** Mirrors com.airlinebookingsystem.dto.assistant.AssistantStatusResponse. */
export interface AssistantStatusDto {
  available: boolean;
}
