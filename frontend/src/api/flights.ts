import { apiClient } from "../lib/apiClient";
import type {
  FlightRequestDto,
  FlightResponseDto,
  FlightSearchRequestDto,
  FlightSearchResponseDto,
  FlightSearchResultDto,
  FlightStatusDto,
  MultiCitySearchRequestDto,
  MultiCitySearchResultDto,
} from "../types/flight";
import type { PagedResponseDto } from "../types/paging";

/**
 * One page of flights, filtered server-side.
 *
 * There is deliberately no "fetch every flight" counterpart: the timetable is
 * several thousand rows and grows with the rolling schedule, so any listing
 * wants a page.
 */
export async function getFlightsPage(
  search: string,
  page: number,
  size: number,
): Promise<PagedResponseDto<FlightResponseDto>> {
  const { data } = await apiClient.get<PagedResponseDto<FlightResponseDto>>(
    "/flights",
    { params: { search, page, size } },
  );
  return data;
}

/** Admin only. */
export async function createFlight(
  request: FlightRequestDto,
): Promise<FlightResponseDto> {
  const { data } = await apiClient.post<FlightResponseDto>("/flights", request);
  return data;
}

/** Admin only. */
export async function updateFlight(
  id: number,
  request: FlightRequestDto,
): Promise<FlightResponseDto> {
  const { data } = await apiClient.put<FlightResponseDto>(
    `/flights/${id}`,
    request,
  );
  return data;
}

/** Admin only. Fails if the flight has bookings. */
export async function deleteFlight(id: number): Promise<void> {
  await apiClient.delete(`/flights/${id}`);
}

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
