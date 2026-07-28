/** Mirrors com.airlinebookingsystem.dto.live.LiveFlightResponse. */
export interface LiveFlightDto {
  icao24: string | null;
  callsign: string;
  originCountry: string | null;
  latitude: number;
  longitude: number;
  altitudeFeet: number | null;
  speedKnots: number | null;
  headingDegrees: number | null;
  onGround: boolean;
}

/** Mirrors com.airlinebookingsystem.dto.live.LiveTrafficResponse. */
export interface LiveTrafficDto {
  flights: LiveFlightDto[];
  totalTracked: number;
  region: string;
  retrievedAt: string;
}
