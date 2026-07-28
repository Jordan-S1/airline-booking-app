import { apiClient } from "../lib/apiClient";
import type { LiveTrafficDto } from "../types/live";

export async function getLiveTraffic(): Promise<LiveTrafficDto> {
  const { data } = await apiClient.get<LiveTrafficDto>("/live-flights");
  return data;
}
