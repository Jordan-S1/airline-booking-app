import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { getCurrencies } from "../api/currency";
import { useAuth } from "./auth";
import type { CurrencyDto } from "../types/currency";

const STORAGE_KEY = "skyair-currency";
const BASE_CURRENCY = "EUR";

interface CurrencyContextValue {
  currencies: CurrencyDto[];
  selectedCode: string;
  setSelectedCode: (code: string) => void;
  /** Formats a EUR-denominated amount into the selected display currency. */
  formatPrice: (eurAmount: number) => string;
}

const CurrencyContext = createContext<CurrencyContextValue | null>(null);

function getInitialCode(): string {
  return localStorage.getItem(STORAGE_KEY) ?? BASE_CURRENCY;
}

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [currencies, setCurrencies] = useState<CurrencyDto[]>([]);
  const [selectedCode, setSelectedCodeState] = useState<string>(getInitialCode);

  useEffect(() => {
    getCurrencies()
      .then(setCurrencies)
      .catch(() => setCurrencies([]));
  }, []);

  // When a user logs in, follow their saved preferred currency.
  useEffect(() => {
    if (user?.preferredCurrency) {
      setSelectedCodeState(user.preferredCurrency);
      localStorage.setItem(STORAGE_KEY, user.preferredCurrency);
    }
  }, [user?.preferredCurrency]);

  const setSelectedCode = (code: string) => {
    setSelectedCodeState(code);
    localStorage.setItem(STORAGE_KEY, code);
  };

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

export function useCurrency() {
  const ctx = useContext(CurrencyContext);
  if (!ctx) throw new Error("useCurrency must be used within a CurrencyProvider");
  return ctx;
}
