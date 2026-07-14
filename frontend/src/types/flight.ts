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

export interface AirportDto {
  code: string;
  city: string;
  country: string;
  timezone: string;
}

export interface FlightStatusDto {
  id: number;
  flightNumber: string;
  airline: string;
  origin: AirportDto;
  destination: AirportDto;
  scheduledDeparture: string;
  scheduledArrival: string;
  estimatedDeparture: string;
  estimatedArrival: string;
  status: FlightStatus;
  progressPercentage: number;
  gate: string;
  terminal: string;
  aircraftType: string;
  altitudeFeet: number;
  speedKnots: number;
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
  departureTime: string;
  arrivalTime: string;
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

export interface WeatherSnapshotDto {
  airportCode: string;
  conditions: string;
  temperatureCelsius: number;
  windSpeedKph: number;
  visibilityKm: number;
  observedAt: string;
}

export interface LoyaltySummaryDto {
  memberTier: "SILVER" | "GOLD" | "PLATINUM" | "OBSIDIAN";
  milesBalance: number;
  milesToNextTier: number;
  upcomingTripCount: number;
}
