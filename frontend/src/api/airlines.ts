import { apiClient } from "../lib/apiClient";
import type { AirlineRequestDto, AirlineResponseDto } from "../types/airline";

export async function getAllAirlines(): Promise<AirlineResponseDto[]> {
  const { data } = await apiClient.get<AirlineResponseDto[]>("/airlines");
  return data;
}

/** Admin only. */
export async function createAirline(
  request: AirlineRequestDto,
): Promise<AirlineResponseDto> {
  const { data } = await apiClient.post<AirlineResponseDto>("/airlines", request);
  return data;
}

/** Admin only. The IATA code is immutable server-side; other fields replace. */
export async function updateAirline(
  id: number,
  request: AirlineRequestDto,
): Promise<AirlineResponseDto> {
  const { data } = await apiClient.put<AirlineResponseDto>(
    `/airlines/${id}`,
    request,
  );
  return data;
}

/** Admin only. Fails if the airline still has flights — deactivate instead. */
export async function deleteAirline(id: number): Promise<void> {
  await apiClient.delete(`/airlines/${id}`);
}

/** Admin only. Takes an airline out of service without deleting its history. */
export async function deactivateAirline(id: number): Promise<void> {
  await apiClient.patch(`/airlines/${id}/deactivate`);
}

/** Admin only. */
export async function reactivateAirline(id: number): Promise<void> {
  await apiClient.patch(`/airlines/${id}/reactivate`);
}
