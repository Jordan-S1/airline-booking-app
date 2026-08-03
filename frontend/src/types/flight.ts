/**
 * Mirrors com.skyair.api.dto.* on the Spring Boot backend.
 * Field names are camelCase; date/time fields are ISO-8601 strings
 * (java.time.LocalDateTime / LocalDate serialized via Jackson).
 */

export type FlightStatus =
  | "SCHEDULED"
  | "BOARDING"
  | "DEPARTED"
  | "IN_AIR"
  | "LANDED"
  | "DELAYED"
  | "CANCELLED";

/** Mirrors com.airlinebookingsystem.entity.Booking.SeatClass on the backend. */
export type SeatClass = "ECONOMY" | "BUSINESS" | "FIRST";

/**
 * Mirrors com.airlinebookingsystem.dto.flight.FlightStatusResponse.
 * `status` and `progressPercentage` are computed server-side from the
 * flight's timetable at request time.
 */
export interface FlightStatusDto {
  id: number;
  flightNumber: string;
  airlineName: string;
  departureAirport: string;
  departureCity: string;
  departureLatitude: number | null;
  departureLongitude: number | null;
  arrivalAirport: string;
  arrivalCity: string;
  arrivalLatitude: number | null;
  arrivalLongitude: number | null;
  /** UTC instant; render via the paired timezone, not the viewer's. */
  departureTime: string;
  arrivalTime: string;
  departureTimezone: string | null;
  arrivalTimezone: string | null;
  duration: number;
  status: FlightStatus;
  progressPercentage: number;
  gate: string | null;
  terminal: string | null;
  aircraft: string | null;
}

/**
 * Mirrors com.airlinebookingsystem.dto.flight.FlightRequest.
 *
 * Times are UTC instants, as everywhere else. The service derives `duration`
 * from the gap between them, so both must sit on the same timeline — sending
 * each end as its own local wall-clock time would produce a duration wrong by
 * the offset between the two airports.
 */
export interface FlightRequestDto {
  flightNumber: string;
  airlineCode: string;
  departureAirportCode: string;
  arrivalAirportCode: string;
  departureTime: string;
  arrivalTime: string;
  economySeats: number;
  businessSeats: number;
  firstClassSeats: number;
  economyPrice: number;
  businessPrice: number | null;
  firstClassPrice: number | null;
  aircraft: string | null;
  status: FlightStatus | null;
}

/** Mirrors com.airlinebookingsystem.dto.flight.FlightResponse. */
export interface FlightResponseDto {
  id: number;
  flightNumber: string;
  airlineCode: string;
  airlineName: string;
  departureAirportCode: string;
  departureCity: string;
  arrivalAirportCode: string;
  arrivalCity: string;
  departureTime: string;
  arrivalTime: string;
  duration: number;
  totalSeats: number;
  availableSeats: number;
  economySeats: number;
  businessSeats: number;
  firstClassSeats: number;
  basePrice: number;
  economyPrice: number;
  businessPrice: number;
  firstClassPrice: number;
  status: FlightStatus;
  active: boolean;
  aircraft: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors com.airlinebookingsystem.dto.flight.FlightSearchRequest. */
export interface FlightSearchRequestDto {
  departureAirport: string;
  arrivalAirport: string;
  departureDate: string;
  returnDate: string | null;
  passengers: number;
  seatClass: SeatClass;
  directFlightsOnly: boolean;
}

/** Mirrors com.airlinebookingsystem.dto.flight.FlightSearchResponse. */
export interface FlightSearchResponseDto {
  id: number;
  flightNumber: string;
  airlineName: string;
  airlineCode: string;
  departureAirport: string;
  arrivalAirport: string;
  departureCity: string;
  arrivalCity: string;
  /** UTC instant; render via the paired timezone, not the viewer's. */
  departureTime: string;
  arrivalTime: string;
  departureTimezone: string | null;
  arrivalTimezone: string | null;
  duration: number;
  price: number;
  availableSeats: number;
  aircraft: string;
}

/** Mirrors com.airlinebookingsystem.dto.flight.FlightSearchResult. */
export interface FlightSearchResultDto {
  outboundFlights: FlightSearchResponseDto[];
  returnFlights: FlightSearchResponseDto[];
  isRoundTrip: boolean;
}

/** Mirrors com.airlinebookingsystem.dto.airport.AirportResponse. */
export interface AirportResponseDto {
  id: number;
  code: string;
  name: string;
  city: string;
  country: string;
  /** ISO 3166-1 alpha-2, used to pick the flag. Null for airports seeded without one. */
  countryCode: string | null;
  timezone: string;
  latitude: number | null;
  longitude: number | null;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors com.airlinebookingsystem.dto.flight.MultiCitySearchRequest. */
export interface MultiCityLegRequestDto {
  departureAirport: string;
  arrivalAirport: string;
  departureDate: string;
}

export interface MultiCitySearchRequestDto {
  legs: MultiCityLegRequestDto[];
  passengers: number;
  seatClass: SeatClass;
  directFlightsOnly: boolean;
}

/** Mirrors com.airlinebookingsystem.dto.flight.MultiCitySearchResult. */
export interface MultiCityLegResultDto {
  legNumber: number;
  departureAirport: string;
  arrivalAirport: string;
  departureDate: string;
  flights: FlightSearchResponseDto[];
}

export interface MultiCitySearchResultDto {
  legs: MultiCityLegResultDto[];
}
