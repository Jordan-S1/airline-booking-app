import { apiClient } from "../lib/apiClient";
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
