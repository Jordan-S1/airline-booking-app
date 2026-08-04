import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Radar } from "lucide-react";
import { getLiveTraffic } from "../api/liveFlights";
import type { LiveFlightDto, LiveTrafficDto } from "../types/live";

type Status = "loading" | "error" | "ready";

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function AircraftRow({ flight, index }: { flight: LiveFlightDto; index: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, delay: Math.min(index * 0.05, 0.4) }}
      className="flex items-center justify-between gap-3 rounded-xl border border-zinc-200 bg-zinc-50 px-3.5 py-2.5 dark:border-white/10 dark:bg-white/[0.03]"
    >
      <div className="min-w-0">
        <p className="truncate font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
          {flight.callsign}
        </p>
        <p className="truncate text-[11px] text-zinc-400 dark:text-zinc-500">
          {flight.originCountry ?? "Unknown origin"}
        </p>
      </div>
      <div className="shrink-0 text-right">
        <p className="font-mono text-xs font-medium text-zinc-600 dark:text-zinc-300">
          {flight.altitudeFeet !== null
            ? `${flight.altitudeFeet.toLocaleString()} ft`
            : "—"}
        </p>
        <p className="font-mono text-[11px] text-zinc-400 dark:text-zinc-500">
          {flight.speedKnots !== null ? `${flight.speedKnots} kt` : "—"}
        </p>
      </div>
    </motion.div>
  );
}

export function LiveTrafficWidget() {
  const [status, setStatus] = useState<Status>("loading");
  const [traffic, setTraffic] = useState<LiveTrafficDto | null>(null);

  useEffect(() => {
    let cancelled = false;

    getLiveTraffic()
      .then((data) => {
        if (!cancelled) {
          setTraffic(data);
          setStatus("ready");
        }
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-zinc-100 text-zinc-900 dark:bg-white/5 dark:text-accent">
            <Radar className="h-5 w-5" strokeWidth={1.8} />
          </span>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
                Live air traffic
              </h2>
              {status === "ready" && (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-cyan-100 px-2.5 py-0.5 text-xs font-medium text-cyan-700 dark:bg-accent/10 dark:text-accent">
                  <span className="relative flex h-1.5 w-1.5">
                    <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-60" />
                    <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-current" />
                  </span>
                  Live
                </span>
              )}
            </div>
            <p className="mt-0.5 text-sm text-zinc-500 dark:text-zinc-400">
              Real aircraft over {traffic?.region ?? "the region"}, via OpenSky
            </p>
          </div>
        </div>

        {status === "ready" && traffic && (
          // Once the header row wraps — just under 535px — this block gets a
          // line to itself, so it spans that line and centres in the card.
          // Above the wrap it sits at the end of the row again, where it is
          // only as wide as "aircraft airborne" and aligns to the right edge.
          <div className="w-full text-center min-[535px]:w-auto min-[535px]:text-right">
            <p className="text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              {traffic.totalTracked}
            </p>
            <p className="text-xs text-zinc-400 dark:text-zinc-500">
              aircraft airborne
            </p>
          </div>
        )}
      </div>

      {status === "loading" && (
        <div className="mt-6 flex items-center gap-2 text-sm text-zinc-400 dark:text-zinc-500">
          <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
          Contacting OpenSky…
        </div>
      )}

      {status === "error" && (
        <p className="mt-6 text-sm text-zinc-400 dark:text-zinc-500">
          Live traffic is unavailable right now.
        </p>
      )}

      {status === "ready" && traffic && traffic.flights.length === 0 && (
        <p className="mt-6 text-sm text-zinc-400 dark:text-zinc-500">
          No aircraft currently transmitting over this region.
        </p>
      )}

      {status === "ready" && traffic && traffic.flights.length > 0 && (
        <>
          <div className="mt-6 grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-3">
            {traffic.flights.slice(0, 6).map((flight, i) => (
              <AircraftRow
                key={`${flight.icao24 ?? flight.callsign}-${i}`}
                flight={flight}
                index={i}
              />
            ))}
          </div>
          <p className="mt-4 text-[11px] text-zinc-400 dark:text-zinc-500">
            Live ADS-B data from the OpenSky Network · highest altitude first ·
            as of {formatTime(traffic.retrievedAt)}
          </p>
        </>
      )}
    </div>
  );
}
