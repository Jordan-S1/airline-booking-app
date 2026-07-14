import type { FlightStatusDto, LoyaltySummaryDto } from "../types/flight";

/**
 * Structured exactly like the JSON a Spring Boot controller would return
 * (e.g. GET /api/flights/{id}/status). Swap this constant for an axios
 * call once the endpoint is live — the shape already matches.
 */
export const mockFlightStatus: FlightStatusDto = {
  id: 48213,
  flightNumber: "SA 217",
  airline: "SkyAir",
  origin: {
    code: "LHR",
    city: "London",
    country: "United Kingdom",
    timezone: "Europe/London",
  },
  destination: {
    code: "JFK",
    city: "New York",
    country: "United States",
    timezone: "America/New_York",
  },
  scheduledDeparture: "2026-07-10T10:35:00",
  scheduledArrival: "2026-07-10T13:50:00",
  estimatedDeparture: "2026-07-10T10:42:00",
  estimatedArrival: "2026-07-10T13:55:00",
  status: "IN_AIR",
  progressPercentage: 62,
  gate: "A14",
  terminal: "5",
  aircraftType: "Airbus A350-1000",
  altitudeFeet: 38000,
  speedKnots: 512,
};

export const mockLoyalty: LoyaltySummaryDto = {
  memberTier: "PLATINUM",
  milesBalance: 84250,
  milesToNextTier: 15750,
  upcomingTripCount: 3,
};
