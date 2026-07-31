import { Link } from "react-router-dom";
import { Luggage } from "lucide-react";
import { useCurrency } from "../lib/currency";
import { parseApiInstant } from "../lib/datetime";
import type { BookingResponseDto } from "../types/booking";

interface TravelStatsWidgetProps {
  status: "loading" | "error" | "ready";
  bookings: BookingResponseDto[];
  isAuthenticated: boolean;
}

interface Stats {
  tripsTaken: number;
  upcoming: number;
  totalSpent: number;
  topDestination: { code: string; visits: number } | null;
}

/** Cancelled bookings are excluded — they were never actually flown or paid. */
function deriveStats(bookings: BookingResponseDto[]): Stats {
  const active = bookings.filter((b) => b.status !== "CANCELLED");
  const now = Date.now();

  const destinationCounts = new Map<string, number>();
  let tripsTaken = 0;
  let upcoming = 0;
  let totalSpent = 0;

  for (const booking of active) {
    totalSpent += booking.totalAmount;

    if (parseApiInstant(booking.arrivalTime).getTime() < now) {
      tripsTaken += 1;
      // Only count somewhere as "visited" once the flight has actually landed.
      destinationCounts.set(
        booking.arrivalAirport,
        (destinationCounts.get(booking.arrivalAirport) ?? 0) + 1,
      );
    } else {
      upcoming += 1;
    }
  }

  let topDestination: Stats["topDestination"] = null;
  for (const [code, visits] of destinationCounts) {
    if (!topDestination || visits > topDestination.visits) {
      topDestination = { code, visits };
    }
  }

  return { tripsTaken, upcoming, totalSpent, topDestination };
}

function StatRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-xs text-zinc-500 dark:text-zinc-400">{label}</span>
      <span className="font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
        {value}
      </span>
    </div>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-full flex-col justify-between rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none">
      {children}
    </div>
  );
}

export function TravelStatsWidget({
  status,
  bookings,
  isAuthenticated,
}: TravelStatsWidgetProps) {
  const { formatPrice } = useCurrency();

  const header = (
    <div className="flex items-start justify-between">
      <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
        Travel stats
      </span>
      <Luggage
        className="h-5 w-5 text-zinc-300 dark:text-zinc-600"
        strokeWidth={1.8}
      />
    </div>
  );

  if (!isAuthenticated) {
    return (
      <Shell>
        {header}
        <div className="mt-6">
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            Sign in to see your travel history and spending.
          </p>
        </div>
      </Shell>
    );
  }

  if (status === "loading") {
    return (
      <Shell>
        {header}
        <div className="mt-6 flex items-center gap-2 text-sm text-zinc-400 dark:text-zinc-500">
          <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
          Loading your stats…
        </div>
      </Shell>
    );
  }

  if (status === "error") {
    return (
      <Shell>
        {header}
        <p className="mt-6 text-sm text-zinc-400 dark:text-zinc-500">
          Stats are unavailable right now.
        </p>
      </Shell>
    );
  }

  const stats = deriveStats(bookings);
  const hasHistory = stats.tripsTaken > 0 || stats.upcoming > 0;

  if (!hasHistory) {
    return (
      <Shell>
        {header}
        <div className="mt-6">
          <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
            No trips yet
          </p>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            Book your first flight and your stats will build up here.
          </p>
        </div>
      </Shell>
    );
  }

  return (
    <Shell>
      {header}

      <div className="mt-4">
        <p className="text-4xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
          {stats.tripsTaken}
        </p>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          {stats.tripsTaken === 1 ? "trip taken" : "trips taken"}
        </p>
      </div>

      <div className="mt-5 flex flex-col gap-2 border-t border-zinc-100 pt-4 dark:border-white/5">
        <StatRow
          label="Upcoming"
          value={String(stats.upcoming)}
        />
        <StatRow label="Total spent" value={formatPrice(stats.totalSpent)} />
        <StatRow
          label="Most visited"
          value={stats.topDestination?.code ?? "—"}
        />
      </div>

      <Link
        to="/trips"
        className="mt-4 text-xs font-medium text-accent transition-opacity hover:opacity-80"
      >
        View all trips →
      </Link>
    </Shell>
  );
}
