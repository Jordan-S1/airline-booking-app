import { createContext, useContext } from "react";
import type { CurrencyDto } from "../types/currency";

export const CURRENCY_STORAGE_KEY = "skyair-currency";
export const BASE_CURRENCY = "EUR";

export interface CurrencyContextValue {
  currencies: CurrencyDto[];
  selectedCode: string;
  setSelectedCode: (code: string) => void;
  /** Formats a EUR-denominated amount into the selected display currency. */
  formatPrice: (eurAmount: number) => string;
}

export const CurrencyContext = createContext<CurrencyContextValue | null>(null);

export function useCurrency() {
  const ctx = useContext(CurrencyContext);
  if (!ctx)
    throw new Error("useCurrency must be used within a CurrencyProvider");
  return ctx;
}
