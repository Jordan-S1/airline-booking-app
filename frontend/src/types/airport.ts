/**
 * Mirrors com.airlinebookingsystem.dto.airport.AirportRequest.
 *
 * Coordinates are optional but strongly wanted: an airport without them has no
 * position for the route map to project, so its flights render on an empty
 * grid. On update, omitting them leaves any existing position untouched.
 */
export interface AirportRequestDto {
  code: string;
  name: string;
  city: string;
  country: string;
  countryCode: string | null;
  timezone: string | null;
  latitude: number | null;
  longitude: number | null;
}
