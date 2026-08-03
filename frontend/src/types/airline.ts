/**
 * Mirrors com.airlinebookingsystem.dto.airline.AirlineRequest.
 * `code` is validated server-side as exactly 2 alphanumeric characters (IATA).
 */
export interface AirlineRequestDto {
  code: string;
  name: string;
  logoUrl: string | null;
  website: string | null;
  country: string;
  active: boolean;
}

/** Mirrors com.airlinebookingsystem.dto.airline.AirlineResponse. */
export interface AirlineResponseDto {
  id: number;
  code: string;
  name: string;
  logoUrl: string | null;
  website: string | null;
  country: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
