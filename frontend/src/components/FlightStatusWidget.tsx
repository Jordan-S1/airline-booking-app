import { motion } from "framer-motion";
import { Plane } from "lucide-react";
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

export function FlightStatusWidget({ flight }: { flight: FlightStatusDto }) {
  const isLive = flight.status === "IN_AIR";
  const progress = Math.min(100, Math.max(0, flight.progressPercentage));

  return (
    <div className="flex h-full flex-col rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8">
      <div className="mb-8 flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2.5">
            <h2 className="font-mono text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
              {flight.flightNumber}
            </h2>
            <span className="text-sm text-zinc-400 dark:text-zinc-500">
              {flight.airline}
            </span>
          </div>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            {flight.aircraftType}
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

      {/* Trajectory */}
      <div className="mb-8 flex items-center gap-4">
        <div className="text-left">
          <p className="font-mono text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {flight.origin.code}
          </p>
          <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
            {flight.origin.city}
          </p>
        </div>

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

        <div className="text-right">
          <p className="font-mono text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {flight.destination.code}
          </p>
          <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
            {flight.destination.city}
          </p>
        </div>
      </div>

      <div className="mb-6 flex items-center justify-between text-sm">
        <div>
          <p className="font-medium text-zinc-900 dark:text-zinc-100">
            {formatTime(flight.estimatedDeparture)}
          </p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">
            Scheduled {formatTime(flight.scheduledDeparture)}
          </p>
        </div>
        <div className="text-right">
          <p className="font-medium text-zinc-900 dark:text-zinc-100">
            {formatTime(flight.estimatedArrival)}
          </p>
          <p className="text-xs text-zinc-400 dark:text-zinc-500">
            Scheduled {formatTime(flight.scheduledArrival)}
          </p>
        </div>
      </div>

      <div className="mt-auto grid grid-cols-2 gap-2.5 sm:grid-cols-4">
        <StatChip label="Gate" value={flight.gate} />
        <StatChip label="Terminal" value={flight.terminal} />
        <StatChip
          label="Altitude"
          value={`${flight.altitudeFeet.toLocaleString()} ft`}
        />
        <StatChip label="Speed" value={`${flight.speedKnots} kt`} />
      </div>
    </div>
  );
}
