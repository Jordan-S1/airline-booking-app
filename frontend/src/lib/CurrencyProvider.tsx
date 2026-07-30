import { useEffect, useMemo, useState, type ReactNode } from "react";
import { getCurrencies } from "../api/currency";
import { useAuth } from "./auth";
import {
  BASE_CURRENCY,
  CurrencyContext,
  CURRENCY_STORAGE_KEY,
  type CurrencyContextValue,
} from "./currency";
import type { CurrencyDto } from "../types/currency";

function getInitialCode(): string {
  return localStorage.getItem(CURRENCY_STORAGE_KEY) ?? BASE_CURRENCY;
}

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [currencies, setCurrencies] = useState<CurrencyDto[]>([]);
  const [selectedCode, setSelectedCode] = useState<string>(getInitialCode);

  // When a user logs in, follow their saved preferred currency. Adjusting
  // during render rather than in an effect avoids a throwaway pass rendering
  // prices in the previous currency before the new one lands.
  const preferred = user?.preferredCurrency;
  const [appliedPreference, setAppliedPreference] = useState(preferred);
  if (preferred && preferred !== appliedPreference) {
    setAppliedPreference(preferred);
    setSelectedCode(preferred);
  }

  useEffect(() => {
    getCurrencies()
      .then(setCurrencies)
      .catch(() => setCurrencies([]));
  }, []);

  useEffect(() => {
    localStorage.setItem(CURRENCY_STORAGE_KEY, selectedCode);
  }, [selectedCode]);

  const value = useMemo<CurrencyContextValue>(() => {
    const active = currencies.find((c) => c.code === selectedCode);
    const rate = active?.rateFromEur ?? 1;
    const code = active?.code ?? BASE_CURRENCY;

    const formatPrice = (eurAmount: number): string => {
      const converted = eurAmount * rate;
      try {
        return new Intl.NumberFormat(undefined, {
          style: "currency",
          currency: code,
        }).format(converted);
      } catch {
        return `${active?.symbol ?? "€"}${converted.toFixed(2)}`;
      }
    };

    return { currencies, selectedCode, setSelectedCode, formatPrice };
  }, [currencies, selectedCode]);

  return (
    <CurrencyContext.Provider value={value}>
      {children}
    </CurrencyContext.Provider>
  );
}
