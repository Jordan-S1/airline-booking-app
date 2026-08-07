import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowLeft, MapPin, Search, X } from "lucide-react";
import { CountryFlag } from "../components/CountryFlag";
import { PageHeader } from "../components/PageHeader";
import { WeatherWidget } from "../components/WeatherWidget";
import {
  FlightResultsList,
  type ResultSection,
} from "../components/FlightResultsList";
import { BookingModal } from "../components/BookingModal";
import { SelectField, type SelectOption } from "../components/SelectField";
import { formatInZone, formatLocalDate } from "../lib/datetime";
import { getAirportByCode } from "../api/airports";
import { getArrivalsAt } from "../api/flights";
import type {
  AirportResponseDto,
  FlightSearchResponseDto,
} from "../types/flight";

type Status = "loading" | "error" | "ready";

/**
 * The departure's local calendar day as YYYY-MM-DD, used to group and filter.
 * Grouping on the departure end rather than the arrival end matches what a
 * traveller is choosing: the day they set off.
 */
function localDayKey(flight: FlightSearchResponseDto): string {
  const [month, day, year] = formatInZone(
    flight.departureTime,
    flight.departureTimezone,
    { year: "numeric", month: "2-digit", day: "2-digit" },
  ).split("/");
  return `${year}-${month}-${day}`;
}

/** A settled load, tagged with the airport code it was made for. */
interface DestinationLoad {
  code: string;
  airport: AirportResponseDto | null;
  flights: FlightSearchResponseDto[];
}

export function DestinationPage() {
  const { code } = useParams<{ code: string }>();
  // Tagging the load with its code lets "loading" be derived rather than
  // assigned, and drops responses for a destination the user has left.
  const [load, setLoad] = useState<DestinationLoad | null>(null);
  const [selectedFlight, setSelectedFlight] =
    useState<FlightSearchResponseDto | null>(null);
  const [filterQuery, setFilterQuery] = useState("");
  const [filterDay, setFilterDay] = useState("");

  useEffect(() => {
    if (!code) return;

    let cancelled = false;

    (async () => {
      try {
        const [airportData, arrivals] = await Promise.all([
          getAirportByCode(code),
          getArrivalsAt(code),
        ]);
        if (!cancelled)
          setLoad({ code, airport: airportData, flights: arrivals });
      } catch {
        if (!cancelled) setLoad({ code, airport: null, flights: [] });
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [code]);

  const current = code && load?.code === code ? load : null;
  const status: Status = !current
    ? "loading"
    : current.airport
      ? "ready"
      : "error";
  const airport = current?.airport ?? null;
  const flights = current?.flights ?? [];

  // The arrivals board spans the whole rolling horizon — a couple of hundred
  // flights over a fortnight — so it needs narrowing down to be usable at all.
  const dayOptions: SelectOption<string>[] = [
    { value: "", label: "Any date" },
    ...[...new Set(flights.map((f) => localDayKey(f)))].sort().map((day) => ({
      value: day,
      label: formatLocalDate(`${day}T12:00:00`, null),
    })),
  ];

  const query = filterQuery.trim().toLowerCase();
  const visibleFlights = flights.filter((flight) => {
    if (filterDay && localDayKey(flight) !== filterDay) return false;
    if (!query) return true;
    return (
      flight.flightNumber.toLowerCase().includes(query) ||
      flight.airlineName.toLowerCase().includes(query) ||
      flight.departureAirport.toLowerCase().includes(query) ||
      flight.departureCity.toLowerCase().includes(query)
    );
  });

  const isFiltered = Boolean(query || filterDay);

  const sections: ResultSection[] = [
    {
      id: "arrivals",
      title: "Arrivals",
      flights: visibleFlights,
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
            subtitle={`${airport.name} (${airport.code}) - every upcoming departure across the network heading here.`}
          />

          <div className="mb-6 grid grid-cols-1 gap-5 sm:grid-cols-3">
            <div className="flex gap-6 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:col-span-2">
              <div className="flex min-w-0 flex-1 flex-col">
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
                {/* mt-auto pins the stat to the bottom so the card fills its height */}
                <div className="mt-auto flex items-baseline gap-2 pt-6">
                  <p className="font-mono text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
                    {flights.length}
                  </p>
                  <span className="text-sm text-zinc-500 dark:text-zinc-400">
                    upcoming {flights.length === 1 ? "flight" : "flights"}{" "}
                    arriving
                  </span>
                </div>
              </div>

              <motion.div
                key={airport.country}
                initial={{ opacity: 0, scale: 0.94 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ type: "spring", stiffness: 240, damping: 22 }}
                className="hidden shrink-0 flex-col items-center justify-center gap-2 sm:flex"
              >
                <CountryFlag
                  countryCode={airport.countryCode}
                  country={airport.country}
                  className="h-24 w-auto rounded-lg border border-zinc-200 shadow-sm dark:border-white/10 dark:shadow-none"
                />
                <span className="flex items-center gap-1 text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                  <MapPin className="h-3 w-3" strokeWidth={2} />
                  {airport.code}
                </span>
              </motion.div>
            </div>

            <WeatherWidget airportCode={airport.code} city={airport.city} />
          </div>

          <FlightResultsList
            status="success"
            errorMessage={null}
            sections={sections}
            heading={`Flights arriving at ${airport.code}`}
            showDates
            priceMode="from"
            emptyMessage={
              isFiltered
                ? "No flights match these filters."
                : `No upcoming flights to ${airport.city} right now.`
            }
            toolbar={
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                <label className="flex min-w-0 flex-1 flex-col gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
                  <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                    Filter
                  </span>
                  <div className="flex items-center gap-2.5">
                    <Search
                      className="h-4 w-4 shrink-0 text-zinc-300 dark:text-zinc-600"
                      strokeWidth={2}
                    />
                    <input
                      value={filterQuery}
                      onChange={(e) => setFilterQuery(e.target.value)}
                      placeholder="Airline, flight number or origin"
                      className="w-full min-w-0 bg-transparent text-sm font-medium text-zinc-900 outline-none placeholder:text-zinc-300 dark:text-zinc-100 dark:placeholder:text-zinc-700"
                    />
                    {filterQuery && (
                      <button
                        type="button"
                        onClick={() => setFilterQuery("")}
                        aria-label="Clear filter"
                        className="shrink-0 cursor-pointer text-zinc-300 transition-colors hover:text-zinc-500 dark:text-zinc-600 dark:hover:text-zinc-400"
                      >
                        <X className="h-4 w-4" strokeWidth={2} />
                      </button>
                    )}
                  </div>
                </label>

                <div className="sm:w-48">
                  <SelectField
                    label="Date"
                    value={filterDay}
                    options={dayOptions}
                    onChange={setFilterDay}
                  />
                </div>
              </div>
            }
            onSelectFlight={(_sectionId, flight) => setSelectedFlight(flight)}
          />

          {isFiltered && visibleFlights.length > 0 && (
            <p className="mt-3 text-center text-xs text-zinc-400 dark:text-zinc-500">
              Showing {visibleFlights.length} of {flights.length} arrivals
            </p>
          )}
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
