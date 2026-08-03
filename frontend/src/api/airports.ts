import { apiClient } from "../lib/apiClient";
import type { AirportRequestDto } from "../types/airport";
import type { AirportResponseDto } from "../types/flight";

export async function searchAirports(
  query: string,
): Promise<AirportResponseDto[]> {
  const { data } = await apiClient.get<AirportResponseDto[]>(
    "/airports/search",
    { params: { query } },
  );
  return data;
}

export async function getAllAirports(): Promise<AirportResponseDto[]> {
  const { data } = await apiClient.get<AirportResponseDto[]>("/airports");
  return data;
}

export async function getAirportByCode(
  code: string,
): Promise<AirportResponseDto> {
  const { data } = await apiClient.get<AirportResponseDto>(
    `/airports/${code}`,
  );
  return data;
}

/** Admin only. */
export async function createAirport(
  request: AirportRequestDto,
): Promise<AirportResponseDto> {
  const { data } = await apiClient.post<AirportResponseDto>(
    "/airports",
    request,
  );
  return data;
}

/** Admin only. The IATA code is immutable server-side; other fields replace. */
export async function updateAirport(
  id: number,
  request: AirportRequestDto,
): Promise<AirportResponseDto> {
  const { data } = await apiClient.put<AirportResponseDto>(
    `/airports/${id}`,
    request,
  );
  return data;
}

/** Admin only. Fails if flights still reference the airport. */
export async function deleteAirport(id: number): Promise<void> {
  await apiClient.delete(`/airports/${id}`);
}
