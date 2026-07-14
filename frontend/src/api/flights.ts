import { apiClient } from "../lib/apiClient";
import type {
  FlightSearchRequestDto,
  FlightSearchResultDto,
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
