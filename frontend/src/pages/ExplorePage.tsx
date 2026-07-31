import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { MapPin, ArrowUpRight, Search, X } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { LiveTrafficWidget } from "../components/LiveTrafficWidget";
import { getAllAirports } from "../api/airports";
import type { AirportResponseDto } from "../types/flight";

type Status = "loading" | "ready" | "error";

export function ExplorePage() {
  const [status, setStatus] = useState<Status>("loading");
  const [airports, setAirports] = useState<AirportResponseDto[]>([]);
  const [query, setQuery] = useState("");

  useEffect(() => {
    getAllAirports()
      .then((data) => {
        setAirports(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, []);

  // Matching the code as well as the names matters more than it looks: someone
  // who already knows they want JFK types the code, not "New York".
  const term = query.trim().toLowerCase();
  const visibleAirports = term
    ? airports.filter(
        (airport) =>
          airport.city.toLowerCase().includes(term) ||
          airport.country.toLowerCase().includes(term) ||
          airport.code.toLowerCase().includes(term) ||
          airport.name.toLowerCase().includes(term),
      )
    : airports;

  return (
    <main className="mx-auto mt-10 max-w-6xl">
      <PageHeader
        eyebrow="Where to next"
        title="Explore destinations"
        subtitle="Browse every airport in the SkyAir network and start planning your next trip."
      />

      <div className="mb-10">
        <LiveTrafficWidget />
      </div>

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
          All destinations
          {status === "ready" && term && (
            <span className="ml-2 text-sm font-normal text-zinc-400 dark:text-zinc-500">
              {visibleAirports.length} of {airports.length}
            </span>
          )}
        </h2>

        {status === "ready" && (
          <label className="flex min-w-0 items-center gap-2.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30 sm:w-80">
            <Search
              className="h-4 w-4 shrink-0 text-zinc-300 dark:text-zinc-600"
              strokeWidth={2}
            />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search city, country or code"
              aria-label="Search destinations"
              className="w-full min-w-0 bg-transparent text-sm font-medium text-zinc-900 outline-none placeholder:text-zinc-300 dark:text-zinc-100 dark:placeholder:text-zinc-700"
            />
            {query && (
              <button
                type="button"
                onClick={() => setQuery("")}
                aria-label="Clear search"
                className="shrink-0 cursor-pointer text-zinc-300 transition-colors hover:text-zinc-500 dark:text-zinc-600 dark:hover:text-zinc-400"
              >
                <X className="h-4 w-4" strokeWidth={2} />
              </button>
            )}
          </label>
        )}
      </div>

      {status === "loading" && (
        <div className="flex items-center gap-3 text-sm text-zinc-500 dark:text-zinc-400">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
          Loading destinations…
        </div>
      )}

      {status === "error" && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
          Could not load destinations. Please try again later.
        </p>
      )}

      {status === "ready" && visibleAirports.length === 0 && (
        <p className="rounded-xl border border-dashed border-zinc-200 px-4 py-8 text-center text-sm text-zinc-500 dark:border-white/10 dark:text-zinc-400">
          No destinations match “{query}”.
        </p>
      )}

      {status === "ready" && visibleAirports.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {visibleAirports.map((airport, i) => (
            <motion.div
              key={airport.id}
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: Math.min(i * 0.04, 0.4) }}
            >
              <Link
                to={`/explore/${airport.code}`}
                className="group flex h-full flex-col justify-between rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm transition-colors hover:border-accent/40 dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none dark:hover:border-accent/40"
              >
                <div className="flex items-start justify-between">
                  <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-zinc-100 text-zinc-900 dark:bg-white/5 dark:text-accent">
                    <MapPin className="h-4 w-4" strokeWidth={1.8} />
                  </span>
                  <span className="font-mono text-lg font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
                    {airport.code}
                  </span>
                </div>
                <div className="mt-6">
                  <h3 className="text-base font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
                    {airport.city}
                  </h3>
                  <p className="mt-0.5 text-sm text-zinc-500 dark:text-zinc-400">
                    {airport.country}
                  </p>
                </div>
                <div className="mt-4 flex items-center gap-1 text-sm font-medium text-zinc-400 transition-colors group-hover:text-accent dark:text-zinc-500">
                  View flights
                  <ArrowUpRight className="h-4 w-4" strokeWidth={1.8} />
                </div>
              </Link>
            </motion.div>
          ))}
        </div>
      )}
    </main>
  );
}
