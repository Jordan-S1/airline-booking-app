import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, MapPin } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { WeatherWidget } from "../components/WeatherWidget";
import {
  FlightResultsList,
  type ResultSection,
} from "../components/FlightResultsList";
import { BookingModal } from "../components/BookingModal";
import { getAirportByCode } from "../api/airports";
import { getArrivalsAt } from "../api/flights";
import type {
  AirportResponseDto,
  FlightSearchResponseDto,
} from "../types/flight";

type Status = "loading" | "error" | "ready";

export function DestinationPage() {
  const { code } = useParams<{ code: string }>();
  const [status, setStatus] = useState<Status>("loading");
  const [airport, setAirport] = useState<AirportResponseDto | null>(null);
  const [flights, setFlights] = useState<FlightSearchResponseDto[]>([]);
  const [selectedFlight, setSelectedFlight] =
    useState<FlightSearchResponseDto | null>(null);

  useEffect(() => {
    if (!code) return;

    let cancelled = false;
    setStatus("loading");

    (async () => {
      try {
        const [airportData, arrivals] = await Promise.all([
          getAirportByCode(code),
          getArrivalsAt(code),
        ]);
        if (!cancelled) {
          setAirport(airportData);
          setFlights(arrivals);
          setStatus("ready");
        }
      } catch {
        if (!cancelled) setStatus("error");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [code]);

  const sections: ResultSection[] = [
    {
      id: "arrivals",
      title: "Arrivals",
      flights,
    },
  ];

  return (
    <main className="mx-auto mt-10 max-w-5xl">
      <Link
        to="/explore"
        className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-zinc-500 transition-colors hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
      >
        <ArrowLeft className="h-4 w-4" strokeWidth={1.8} />
        All destinations
      </Link>

      {status === "error" && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
          We couldn't find that destination.
        </p>
      )}

      {status === "loading" && (
        <div className="flex items-center gap-3 py-8 text-sm text-zinc-500 dark:text-zinc-400">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
          Loading destination…
        </div>
      )}

      {status === "ready" && airport && (
        <>
          <PageHeader
            eyebrow={airport.country}
            title={`Flights to ${airport.city}`}
            subtitle={`${airport.name} (${airport.code}) — every upcoming departure across the network heading here.`}
          />

          <div className="mb-6 grid grid-cols-1 gap-5 sm:grid-cols-3">
            <div className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:col-span-2">
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                    Destination
                  </span>
                  <p className="mt-1 text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
                    {airport.city}
                  </p>
                  <p className="mt-0.5 text-sm text-zinc-500 dark:text-zinc-400">
                    {airport.country} · {airport.timezone}
                  </p>
                </div>
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-zinc-100 text-zinc-900 dark:bg-white/5 dark:text-accent">
                  <MapPin className="h-5 w-5" strokeWidth={1.8} />
                </span>
              </div>
              <div className="mt-6 flex items-baseline gap-2">
                <p className="font-mono text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
                  {flights.length}
                </p>
                <span className="text-sm text-zinc-500 dark:text-zinc-400">
                  upcoming {flights.length === 1 ? "flight" : "flights"} arriving
                </span>
              </div>
            </div>

            <WeatherWidget airportCode={airport.code} city={airport.city} />
          </div>

          <FlightResultsList
            status="success"
            errorMessage={null}
            sections={sections}
            heading={`Flights arriving at ${airport.code}`}
            emptyMessage={`No upcoming flights to ${airport.city} right now.`}
            onSelectFlight={(_sectionId, flight) => setSelectedFlight(flight)}
          />
        </>
      )}

      {selectedFlight && (
        <BookingModal
          flights={[selectedFlight]}
          passengerCount={1}
          seatClass="ECONOMY"
          onClose={() => setSelectedFlight(null)}
        />
      )}
    </main>
  );
}
