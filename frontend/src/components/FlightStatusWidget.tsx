import { motion } from "framer-motion";
import { ArrowRight, Plane } from "lucide-react";
import { RouteMap } from "./RouteMap";
import type { FlightStatus, FlightStatusDto } from "../types/flight";

const STATUS_STYLES: Record<FlightStatus, string> = {
  SCHEDULED: "bg-zinc-100 text-zinc-600 dark:bg-white/5 dark:text-zinc-400",
  BOARDING:
    "bg-amber-100 text-amber-700 dark:bg-amber-400/10 dark:text-amber-400",
  DEPARTED: "bg-blue-100 text-blue-700 dark:bg-blue-400/10 dark:text-blue-400",
  IN_AIR: "bg-cyan-100 text-cyan-700 dark:bg-accent/10 dark:text-accent",
  LANDED:
    "bg-emerald-100 text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-400",
  DELAYED:
    "bg-orange-100 text-orange-700 dark:bg-orange-400/10 dark:text-orange-400",
  CANCELLED: "bg-red-100 text-red-700 dark:bg-red-400/10 dark:text-red-400",
};

const STATUS_LABELS: Record<FlightStatus, string> = {
  SCHEDULED: "Scheduled",
  BOARDING: "Boarding",
  DEPARTED: "Departed",
  IN_AIR: "In flight",
  LANDED: "Landed",
  DELAYED: "Delayed",
  CANCELLED: "Cancelled",
};

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", {
    weekday: "short",
    day: "numeric",
    month: "short",
  });
}

function formatDuration(minutes: number) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}h ${m}m`;
}

function StatChip({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5 rounded-xl border border-zinc-200 bg-zinc-50 px-3.5 py-2.5 dark:border-white/10 dark:bg-white/[0.03]">
      <span className="text-[10px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
        {label}
      </span>
      <span className="font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
        {value}
      </span>
    </div>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-full flex-col rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8">
      {children}
    </div>
  );
}

interface FlightStatusWidgetProps {
  status: "loading" | "error" | "empty" | "ready";
  flight: FlightStatusDto | null;
  isAuthenticated: boolean;
  /** Empty-state CTA when signed in — the page decides where the search lives. */
  onFindFlights: () => void;
  /** Empty-state CTA when signed out; there's nothing to track until you log in. */
  onSignIn: () => void;
}

export function FlightStatusWidget({
  status,
  flight,
  isAuthenticated,
  onFindFlights,
  onSignIn,
}: FlightStatusWidgetProps) {
  if (status === "loading") {
    return (
      <Shell>
        <div className="flex flex-1 items-center gap-3 text-sm text-zinc-500 dark:text-zinc-400">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
          Loading your next flight…
        </div>
      </Shell>
    );
  }

  if (status === "error") {
    return (
      <Shell>
        <div className="flex flex-1 items-center text-sm text-zinc-500 dark:text-zinc-400">
          Flight status is unavailable right now.
        </div>
      </Shell>
    );
  }

  if (status === "empty" || !flight) {
    return (
      <Shell>
        <div className="flex flex-1 flex-col items-center justify-center gap-3 py-6 text-center">
          <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-zinc-100 text-zinc-400 dark:bg-white/5 dark:text-zinc-500">
            <Plane className="h-5 w-5 rotate-45" strokeWidth={1.8} />
          </span>
          <div>
            <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
              {isAuthenticated ? "No upcoming flights" : "No flight to track"}
            </p>
            <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
              {isAuthenticated
                ? "Book a flight and its live status will appear here."
                : "Sign in to track the status of your booked flights."}
            </p>
          </div>
          {/* The CTA has to match the copy above it: signing in is the blocker
              when signed out, searching is the next step once you are. */}
          <button
            type="button"
            onClick={isAuthenticated ? onFindFlights : onSignIn}
            className="group mt-1 inline-flex cursor-pointer items-center gap-1.5 text-sm font-medium text-accent transition-opacity hover:opacity-80"
          >
            {isAuthenticated ? "Search flights" : "Sign in"}
            <ArrowRight
              className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5"
              strokeWidth={2}
            />
          </button>
        </div>
      </Shell>
    );
  }

  const isLive = flight.status === "IN_AIR";
  const progress = Math.min(100, Math.max(0, flight.progressPercentage));

  return (
    <Shell>
      <div className="mb-8 flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2.5">
            <h2 className="font-mono text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
              {flight.flightNumber}
            </h2>
            <span className="text-sm text-zinc-400 dark:text-zinc-500">
              {flight.airlineName}
            </span>
          </div>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            {flight.aircraft ?? "Aircraft TBC"} ·{" "}
            {formatDate(flight.departureTime)}
          </p>
        </div>

        <span
          className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium ${STATUS_STYLES[flight.status]}`}
        >
          {isLive && (
            <span className="relative flex h-1.5 w-1.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-60" />
              <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-current" />
            </span>
          )}
          {STATUS_LABELS[flight.status]}
        </span>
      </div>

      {/* Route map, or a linear trajectory when the airports have no coordinates */}
      {flight.departureLatitude !== null && flight.arrivalLatitude !== null ? (
        <div className="mb-6">
          <RouteMap
            departureCode={flight.departureAirport}
            arrivalCode={flight.arrivalAirport}
            departureLatitude={flight.departureLatitude}
            departureLongitude={flight.departureLongitude}
            arrivalLatitude={flight.arrivalLatitude}
            arrivalLongitude={flight.arrivalLongitude}
            progressPercentage={progress}
          />
        </div>
      ) : (
        <div className="mb-8 flex items-center gap-4">
          <p className="font-mono text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {flight.departureAirport}
          </p>
          <div className="relative flex-1">
            <div className="h-px w-full bg-zinc-200 dark:bg-white/10" />
            <motion.div
              className="absolute inset-y-0 left-0 h-px bg-accent"
              initial={{ width: 0 }}
              animate={{ width: `${progress}%` }}
              transition={{ duration: 1.2, ease: "easeOut" }}
            />
            <motion.div
              className="absolute top-1/2 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full bg-white text-accent shadow-md ring-1 ring-zinc-200 dark:bg-obsidian-raised dark:ring-white/10"
              initial={{ left: 0 }}
              animate={{ left: `calc(${progress}% - 14px)` }}
              transition={{ duration: 1.2, ease: "easeOut" }}
            >
              <Plane className="h-3.5 w-3.5 rotate-45" fill="currentColor" />
            </motion.div>
          </div>
          <p className="font-mono text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {flight.arrivalAirport}
          </p>
        </div>
      )}

      <div className="mb-6 flex items-center justify-between text-sm">
        <div>
          <p className="font-medium text-zinc-900 dark:text-zinc-100">
            {formatTime(flight.departureTime)}
          </p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">
            {flight.departureCity}
          </p>
        </div>
        <div className="text-right">
          <p className="font-medium text-zinc-900 dark:text-zinc-100">
            {formatTime(flight.arrivalTime)}
          </p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">
            {flight.arrivalCity}
          </p>
        </div>
      </div>

      <div className="mt-auto grid grid-cols-2 gap-2.5 sm:grid-cols-4">
        <StatChip label="Gate" value={flight.gate ?? "—"} />
        <StatChip label="Terminal" value={flight.terminal ?? "—"} />
        <StatChip label="Duration" value={formatDuration(flight.duration)} />
        <StatChip label="Progress" value={`${progress}%`} />
      </div>
    </Shell>
  );
}
