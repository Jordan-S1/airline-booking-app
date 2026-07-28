import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ArrowLeftRight, ArrowRight, ChevronDown, Plus, X } from "lucide-react";
import { AirportAutocomplete } from "./AirportAutocomplete";
import type {
  FlightSearchRequestDto,
  MultiCityLegRequestDto,
  SeatClass,
} from "../types/flight";

export type TripType = "ONE_WAY" | "ROUND_TRIP" | "MULTI_CITY";

const TRIP_TYPES: { value: TripType; label: string }[] = [
  { value: "ONE_WAY", label: "One way" },
  { value: "ROUND_TRIP", label: "Round trip" },
  { value: "MULTI_CITY", label: "Multi-city" },
];

const SEAT_CLASSES: { value: SeatClass; label: string }[] = [
  { value: "ECONOMY", label: "Economy" },
  { value: "BUSINESS", label: "Business" },
  { value: "FIRST", label: "First" },
];

const POPULAR_ROUTES: { departureAirport: string; arrivalAirport: string }[] = [
  { departureAirport: "DUB", arrivalAirport: "LHR" },
  { departureAirport: "DUB", arrivalAirport: "CDG" },
  { departureAirport: "LHR", arrivalAirport: "MAD" },
  { departureAirport: "DUB", arrivalAirport: "BCN" },
];

const MAX_LEGS = 5;
const DEFAULT_DATE = "2026-08-01";

function SelectChevron() {
  return (
    <ChevronDown
      strokeWidth={1.8}
      className="pointer-events-none absolute right-0 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400 dark:text-zinc-500"
    />
  );
}

function FieldShell({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="group flex flex-1 flex-col gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
      <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
        {label}
      </span>
      {children}
    </label>
  );
}

export interface SearchSubmission {
  tripType: TripType;
  simple: FlightSearchRequestDto;
  legs: MultiCityLegRequestDto[];
}

interface FlightSearchPanelProps {
  onSearch: (submission: SearchSubmission) => void;
  isSearching: boolean;
}

export function FlightSearchPanel({
  onSearch,
  isSearching,
}: FlightSearchPanelProps) {
  const [tripType, setTripType] = useState<TripType>("ONE_WAY");
  const [request, setRequest] = useState<FlightSearchRequestDto>({
    departureAirport: "DUB",
    arrivalAirport: "LHR",
    departureDate: DEFAULT_DATE,
    returnDate: null,
    passengers: 1,
    seatClass: "BUSINESS",
    directFlightsOnly: false,
  });
  const [legs, setLegs] = useState<MultiCityLegRequestDto[]>([
    { departureAirport: "DUB", arrivalAirport: "CDG", departureDate: DEFAULT_DATE },
    { departureAirport: "CDG", arrivalAirport: "FCO", departureDate: "2026-08-04" },
  ]);

  const isMultiCity = tripType === "MULTI_CITY";
  const isRoundTrip = tripType === "ROUND_TRIP";

  const handleSwap = () => {
    setRequest((prev) => ({
      ...prev,
      departureAirport: prev.arrivalAirport,
      arrivalAirport: prev.departureAirport,
    }));
  };

  const handleTripTypeChange = (next: TripType) => {
    setTripType(next);
    // A return date only makes sense for a round trip — clear it otherwise so
    // the backend doesn't treat the search as one.
    setRequest((prev) => ({
      ...prev,
      returnDate: next === "ROUND_TRIP" ? (prev.returnDate ?? "2026-08-08") : null,
    }));
  };

  const updateLeg = (
    index: number,
    field: keyof MultiCityLegRequestDto,
    value: string,
  ) => {
    setLegs((prev) =>
      prev.map((leg, i) => (i === index ? { ...leg, [field]: value } : leg)),
    );
  };

  const addLeg = () => {
    setLegs((prev) => {
      if (prev.length >= MAX_LEGS) return prev;
      const last = prev[prev.length - 1];
      return [
        ...prev,
        {
          departureAirport: last.arrivalAirport,
          arrivalAirport: "",
          departureDate: last.departureDate,
        },
      ];
    });
  };

  const removeLeg = (index: number) => {
    setLegs((prev) => (prev.length <= 2 ? prev : prev.filter((_, i) => i !== index)));
  };

  return (
    <div className="flex h-full flex-col justify-between rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8">
      <div>
        <div className="mb-4">
          <h2 className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
            Book a flight
          </h2>
          <p className="mt-0.5 text-sm text-zinc-500 dark:text-zinc-400">
            Search live availability across the SkyAir network.
          </p>
        </div>

        {/* Trip type */}
        <div className="mb-4 flex gap-1 rounded-xl border border-zinc-200 p-1 dark:border-white/10">
          {TRIP_TYPES.map((type) => (
            <button
              key={type.value}
              type="button"
              onClick={() => handleTripTypeChange(type.value)}
              className={`flex-1 cursor-pointer rounded-lg py-1.5 text-xs font-medium transition-colors ${
                tripType === type.value
                  ? "bg-zinc-900 text-white dark:bg-white dark:text-zinc-900"
                  : "text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
              }`}
            >
              {type.label}
            </button>
          ))}
        </div>

        <div className="flex flex-col gap-3">
          <AnimatePresence mode="wait" initial={false}>
            {isMultiCity ? (
              <motion.div
                key="multi"
                initial={{ opacity: 0, y: -6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -6 }}
                transition={{ duration: 0.18 }}
                className="flex flex-col gap-3"
              >
                {legs.map((leg, index) => (
                  <div
                    key={index}
                    className="rounded-xl border border-zinc-200 p-3 dark:border-white/10"
                  >
                    <div className="mb-2 flex items-center justify-between">
                      <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                        Leg {index + 1}
                      </span>
                      {legs.length > 2 && (
                        <button
                          type="button"
                          onClick={() => removeLeg(index)}
                          aria-label={`Remove leg ${index + 1}`}
                          className="cursor-pointer text-zinc-300 transition-colors hover:text-red-400 dark:text-zinc-600"
                        >
                          <X className="h-3.5 w-3.5" />
                        </button>
                      )}
                    </div>
                    <div className="flex flex-col gap-2">
                      <div className="flex gap-2">
                        <AirportAutocomplete
                          label="From"
                          value={leg.departureAirport}
                          onChange={(code) =>
                            updateLeg(index, "departureAirport", code)
                          }
                          placeholder="DUB"
                        />
                        <AirportAutocomplete
                          label="To"
                          value={leg.arrivalAirport}
                          onChange={(code) =>
                            updateLeg(index, "arrivalAirport", code)
                          }
                          placeholder="CDG"
                        />
                      </div>
                      <FieldShell label="Date">
                        <input
                          type="date"
                          value={leg.departureDate}
                          onChange={(e) =>
                            updateLeg(index, "departureDate", e.target.value)
                          }
                          className="w-full bg-transparent text-sm font-medium text-zinc-900 outline-none [color-scheme:light] dark:text-zinc-100 dark:[color-scheme:dark]"
                        />
                      </FieldShell>
                    </div>
                  </div>
                ))}

                {legs.length < MAX_LEGS && (
                  <button
                    type="button"
                    onClick={addLeg}
                    className="flex cursor-pointer items-center justify-center gap-1.5 rounded-xl border border-dashed border-zinc-300 py-2.5 text-sm font-medium text-zinc-500 transition-colors hover:border-accent/40 hover:text-accent dark:border-white/15 dark:text-zinc-400"
                  >
                    <Plus className="h-4 w-4" strokeWidth={2} />
                    Add another leg
                  </button>
                )}
              </motion.div>
            ) : (
              <motion.div
                key="simple"
                initial={{ opacity: 0, y: -6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -6 }}
                transition={{ duration: 0.18 }}
                className="flex flex-col gap-3"
              >
                <div className="relative flex flex-col gap-3 sm:flex-row sm:items-center">
                  <AirportAutocomplete
                    label="Origin"
                    value={request.departureAirport}
                    onChange={(code) =>
                      setRequest((p) => ({ ...p, departureAirport: code }))
                    }
                    placeholder="LHR"
                  />

                  <motion.button
                    type="button"
                    onClick={handleSwap}
                    whileTap={{ scale: 0.9, rotate: 180 }}
                    aria-label="Swap origin and destination"
                    className="z-10 flex h-9 w-9 shrink-0 cursor-pointer items-center justify-center self-center rounded-full border border-zinc-200 bg-white text-zinc-500 shadow-sm transition-colors hover:text-accent dark:border-white/10 dark:bg-obsidian-raised dark:text-zinc-400 sm:absolute sm:left-1/2 sm:-translate-x-1/2"
                  >
                    <ArrowLeftRight className="h-4 w-4" strokeWidth={1.8} />
                  </motion.button>

                  <AirportAutocomplete
                    label="Destination"
                    value={request.arrivalAirport}
                    onChange={(code) =>
                      setRequest((p) => ({ ...p, arrivalAirport: code }))
                    }
                    placeholder="JFK"
                  />
                </div>

                <div className="flex flex-col gap-3 sm:flex-row">
                  <FieldShell label="Departure">
                    <input
                      type="date"
                      value={request.departureDate}
                      onChange={(e) =>
                        setRequest((p) => ({
                          ...p,
                          departureDate: e.target.value,
                        }))
                      }
                      className="w-full bg-transparent text-sm font-medium text-zinc-900 outline-none [color-scheme:light] dark:text-zinc-100 dark:[color-scheme:dark]"
                    />
                  </FieldShell>

                  {isRoundTrip && (
                    <FieldShell label="Return">
                      <input
                        type="date"
                        value={request.returnDate ?? ""}
                        min={request.departureDate}
                        onChange={(e) =>
                          setRequest((p) => ({
                            ...p,
                            returnDate: e.target.value || null,
                          }))
                        }
                        className="w-full bg-transparent text-sm font-medium text-zinc-900 outline-none [color-scheme:light] dark:text-zinc-100 dark:[color-scheme:dark]"
                      />
                    </FieldShell>
                  )}
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          <div className="flex flex-col gap-3 sm:flex-row">
            <FieldShell label="Passengers">
              <div className="relative pr-5">
                <select
                  value={request.passengers}
                  onChange={(e) =>
                    setRequest((p) => ({
                      ...p,
                      passengers: Number(e.target.value),
                    }))
                  }
                  className="w-full cursor-pointer appearance-none bg-transparent text-sm font-medium text-zinc-900 outline-none dark:text-zinc-100 [&>option]:text-zinc-900"
                >
                  {[1, 2, 3, 4, 5, 6].map((n) => (
                    <option key={n} value={n}>
                      {n} {n === 1 ? "passenger" : "passengers"}
                    </option>
                  ))}
                </select>
                <SelectChevron />
              </div>
            </FieldShell>

            <FieldShell label="Cabin">
              <div className="relative pr-5">
                <select
                  value={request.seatClass}
                  onChange={(e) =>
                    setRequest((p) => ({
                      ...p,
                      seatClass: e.target.value as SeatClass,
                    }))
                  }
                  className="w-full cursor-pointer appearance-none bg-transparent text-sm font-medium text-zinc-900 outline-none dark:text-zinc-100 [&>option]:text-zinc-900"
                >
                  {SEAT_CLASSES.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
                <SelectChevron />
              </div>
            </FieldShell>
          </div>

          {!isMultiCity && (
            <div className="pt-2">
              <p className="mb-2.5 text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                Popular routes
              </p>
              <div className="flex flex-wrap gap-2">
                {POPULAR_ROUTES.map((route) => (
                  <button
                    key={`${route.departureAirport}-${route.arrivalAirport}`}
                    type="button"
                    onClick={() =>
                      setRequest((p) => ({
                        ...p,
                        departureAirport: route.departureAirport,
                        arrivalAirport: route.arrivalAirport,
                      }))
                    }
                    className="cursor-pointer rounded-lg border border-zinc-200 px-3 py-1.5 font-mono text-xs font-medium text-zinc-600 transition-colors hover:border-accent/40 hover:text-accent dark:border-white/10 dark:text-zinc-400 dark:hover:border-accent/40"
                  >
                    {route.departureAirport} → {route.arrivalAirport}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      <motion.button
        type="button"
        onClick={() => onSearch({ tripType, simple: request, legs })}
        disabled={isSearching}
        whileHover={{ scale: isSearching ? 1 : 1.01 }}
        whileTap={{ scale: isSearching ? 1 : 0.98 }}
        className="mt-6 flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl bg-zinc-900 py-3.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
      >
        {isSearching ? (
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white dark:border-zinc-900/30 dark:border-t-zinc-900" />
        ) : (
          <>
            Search flights
            <ArrowRight className="h-4 w-4" strokeWidth={2} />
          </>
        )}
      </motion.button>
    </div>
  );
}
