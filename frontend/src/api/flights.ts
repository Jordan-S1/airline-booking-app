import { apiClient } from "../lib/apiClient";
import type {
  FlightSearchRequestDto,
  FlightSearchResponseDto,
  FlightSearchResultDto,
  FlightStatusDto,
  MultiCitySearchRequestDto,
  MultiCitySearchResultDto,
} from "../types/flight";

export async function searchFlights(
  request: FlightSearchRequestDto,
): Promise<FlightSearchResultDto> {
  const { data } = await apiClient.post<FlightSearchResultDto>(
    "/flights/search",
    request,
  );
  return data;
}

export async function searchMultiCity(
  request: MultiCitySearchRequestDto,
): Promise<MultiCitySearchResultDto> {
  const { data } = await apiClient.post<MultiCitySearchResultDto>(
    "/flights/search/multi-city",
    request,
  );
  return data;
}

export async function getArrivalsAt(
  airportCode: string,
): Promise<FlightSearchResponseDto[]> {
  const { data } = await apiClient.get<FlightSearchResponseDto[]>(
    `/flights/arrivals/${airportCode}`,
  );
  return data;
}

export async function getFlightStatus(
  flightId: number,
): Promise<FlightStatusDto> {
  const { data } = await apiClient.get<FlightStatusDto>(
    `/flights/${flightId}/status`,
  );
  return data;
}
