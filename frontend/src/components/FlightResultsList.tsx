import { motion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import type { FlightSearchResponseDto } from "../types/flight";

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatDuration(minutes: number) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}h ${m}m`;
}

function FlightResultCard({
  flight,
  onSelect,
}: {
  flight: FlightSearchResponseDto;
  onSelect: (flight: FlightSearchResponseDto) => void;
}) {
  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="flex flex-col gap-4 rounded-xl border border-zinc-200 bg-white p-4 transition-colors hover:border-zinc-300 dark:border-white/10 dark:bg-white/[0.03] dark:hover:border-white/20 sm:flex-row sm:items-center sm:justify-between"
    >
      <div className="flex items-center gap-4">
        <div>
          <p className="font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
            {flight.flightNumber}
          </p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">
            {flight.airlineName}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="text-right">
            <p className="font-mono text-lg font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              {formatTime(flight.departureTime)}
            </p>
            <p className="text-xs text-zinc-400 dark:text-zinc-500">
              {flight.departureAirport}
            </p>
          </div>
          <div className="flex flex-col items-center px-1 text-zinc-300 dark:text-zinc-600">
            <span className="text-[10px]">{formatDuration(flight.duration)}</span>
            <ArrowRight className="h-3 w-8" strokeWidth={1.5} />
          </div>
          <div>
            <p className="font-mono text-lg font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              {formatTime(flight.arrivalTime)}
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
            ${flight.price.toLocaleString()}
          </p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">
            {flight.availableSeats} seats left
          </p>
        </div>
        <button
          type="button"
          onClick={() => onSelect(flight)}
          className="rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
        >
          Select
        </button>
      </div>
    </motion.div>
  );
}

interface FlightResultsListProps {
  status: "idle" | "loading" | "error" | "success";
  errorMessage: string | null;
  outboundFlights: FlightSearchResponseDto[];
  onSelectFlight: (flight: FlightSearchResponseDto) => void;
}

export function FlightResultsList({
  status,
  errorMessage,
  outboundFlights,
  onSelectFlight,
}: FlightResultsListProps) {
  if (status === "idle") return null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="mt-5 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8"
    >
      <h2 className="mb-4 text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
        Search results
      </h2>

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

      {status === "success" && outboundFlights.length === 0 && (
        <p className="py-6 text-sm text-zinc-500 dark:text-zinc-400">
          No flights found for this route and date.
        </p>
      )}

      {status === "success" && outboundFlights.length > 0 && (
        <div className="flex flex-col gap-3">
          {outboundFlights.map((flight) => (
            <FlightResultCard
              key={flight.id}
              flight={flight}
              onSelect={onSelectFlight}
            />
          ))}
        </div>
      )}
    </motion.div>
  );
}
