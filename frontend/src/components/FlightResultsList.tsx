import { motion } from "framer-motion";
import { ArrowRight, Check } from "lucide-react";
import { useCurrency } from "../lib/currency";
import {
  arrivalDayOffset,
  formatLocalDate,
  formatLocalTime,
} from "../lib/datetime";
import type { FlightSearchResponseDto } from "../types/flight";

function formatDuration(minutes: number) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}h ${m}m`;
}

function FlightResultCard({
  flight,
  onSelect,
  isSelected,
  selectLabel,
  showDate,
}: {
  flight: FlightSearchResponseDto;
  onSelect: (flight: FlightSearchResponseDto) => void;
  isSelected: boolean;
  selectLabel: string;
  showDate: boolean;
}) {
  const { formatPrice } = useCurrency();
  const dayOffset = arrivalDayOffset(flight);
  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className={`flex flex-col gap-4 rounded-xl border bg-white p-4 transition-colors dark:bg-white/[0.03] sm:flex-row sm:items-center sm:justify-between ${
        isSelected
          ? "border-accent/50 ring-1 ring-accent/30"
          : "border-zinc-200 hover:border-zinc-300 dark:border-white/10 dark:hover:border-white/20"
      }`}
    >
      <div className="flex items-center gap-4">
        <div>
          <p className="font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
            {flight.flightNumber}
          </p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">
            {flight.airlineName}
          </p>
          {/* A search already fixes the date, so it is only shown where the
              list spans many days — the arrivals board on a destination. */}
          {showDate && (
            <p className="mt-0.5 text-xs font-medium text-zinc-500 dark:text-zinc-400">
              {formatLocalDate(flight.departureTime, flight.departureTimezone)}
            </p>
          )}
        </div>

        <div className="flex items-center gap-3">
          <div className="text-right">
            <p className="font-mono text-lg font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              {formatLocalTime(flight.departureTime, flight.departureTimezone)}
            </p>
            <p className="text-xs text-zinc-400 dark:text-zinc-500">
              {flight.departureAirport}
            </p>
          </div>
          <div className="flex flex-col items-center px-1 text-zinc-300 dark:text-zinc-600">
            <span className="text-[10px]">
              {formatDuration(flight.duration)}
            </span>
            <ArrowRight className="h-3 w-8" strokeWidth={1.5} />
          </div>
          <div>
            <p className="font-mono text-lg font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              {formatLocalTime(flight.arrivalTime, flight.arrivalTimezone)}
              {/* Overnight arrivals need the day marker, or the pair of times
                  reads as landing before take-off. */}
              {dayOffset > 0 && (
                <sup
                  className="ml-0.5 text-[10px] font-semibold text-accent"
                  title={`Arrives ${dayOffset} day${dayOffset > 1 ? "s" : ""} later`}
                >
                  +{dayOffset}
                </sup>
              )}
            </p>
            <p className="text-xs text-zinc-400 dark:text-zinc-500">
              {flight.arrivalAirport}
            </p>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between gap-4 sm:justify-end">
        <div className="text-right">
          <p className="text-lg font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {formatPrice(flight.price)}
          </p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">
            {flight.availableSeats} seats left
          </p>
        </div>
        <button
          type="button"
          onClick={() => onSelect(flight)}
          className={`flex cursor-pointer items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-medium transition-colors pointer-coarse:min-h-11 ${
            isSelected
              ? "bg-accent/10 text-accent"
              : "bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
          }`}
        >
          {isSelected && <Check className="h-3.5 w-3.5" strokeWidth={2.5} />}
          {isSelected ? "Selected" : selectLabel}
        </button>
      </div>
    </motion.div>
  );
}

export interface ResultSection {
  /** Stable key — the leg index for multi-city, or "outbound"/"return". */
  id: string;
  title: string;
  subtitle?: string;
  flights: FlightSearchResponseDto[];
  selectedFlightId?: number | null;
}

interface FlightResultsListProps {
  status: "idle" | "loading" | "error" | "success";
  errorMessage: string | null;
  sections: ResultSection[];
  onSelectFlight: (sectionId: string, flight: FlightSearchResponseDto) => void;
  /** Rendered under the sections — e.g. the multi-leg "continue" bar. */
  footer?: React.ReactNode;
  /** Defaults to "Search results"; overridden when reused outside the dashboard. */
  heading?: string;
  emptyMessage?: string;
  /** Rendered between the heading and the results — e.g. a filter bar. */
  toolbar?: React.ReactNode;
  /**
   * Show each flight's departure date. Off for searches, where the date is
   * already fixed by the query; on for lists that span the whole horizon.
   */
  showDates?: boolean;
}

export function FlightResultsList({
  status,
  errorMessage,
  sections,
  onSelectFlight,
  footer,
  heading = "Search results",
  emptyMessage = "No flights found for this route and date.",
  toolbar,
  showDates = false,
}: FlightResultsListProps) {
  if (status === "idle") return null;

  const isMultiSection = sections.length > 1;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="mt-5 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8"
    >
      <h2 className="mb-4 text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
        {heading}
      </h2>

      {toolbar && <div className="mb-5">{toolbar}</div>}

      {status === "loading" && (
        <div className="flex items-center gap-3 py-6 text-sm text-zinc-500 dark:text-zinc-400">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
          Searching live availability…
        </div>
      )}

      {status === "error" && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
          {errorMessage ?? "Something went wrong while searching flights."}
        </p>
      )}

      {status === "success" && (
        <div className="flex flex-col gap-8">
          {sections.map((section) => (
            <div key={section.id}>
              {isMultiSection && (
                <div className="mb-3 flex items-baseline justify-between">
                  <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                    {section.title}
                  </h3>
                  {section.subtitle && (
                    <span className="text-xs text-zinc-400 dark:text-zinc-500">
                      {section.subtitle}
                    </span>
                  )}
                </div>
              )}

              {section.flights.length === 0 ? (
                <p className="rounded-xl border border-dashed border-zinc-200 px-4 py-6 text-center text-sm text-zinc-500 dark:border-white/10 dark:text-zinc-400">
                  {emptyMessage}
                </p>
              ) : (
                <div className="flex flex-col gap-3">
                  {section.flights.map((flight) => (
                    <FlightResultCard
                      key={flight.id}
                      flight={flight}
                      isSelected={section.selectedFlightId === flight.id}
                      selectLabel={isMultiSection ? "Select" : "Book"}
                      showDate={showDates}
                      onSelect={(f) => onSelectFlight(section.id, f)}
                    />
                  ))}
                </div>
              )}
            </div>
          ))}

          {footer}
        </div>
      )}
    </motion.div>
  );
}
