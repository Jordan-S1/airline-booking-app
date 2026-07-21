import { apiClient } from "../lib/apiClient";
import type { CurrencyDto } from "../types/currency";

export async function getCurrencies(): Promise<CurrencyDto[]> {
  const { data } = await apiClient.get<CurrencyDto[]>("/currencies");
  return data;
}
