import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import {
  Ban,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Pencil,
  Plus,
  Search,
  Trash2,
} from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { SelectField, type SelectOption } from "../components/SelectField";
import { AirlineForm } from "../components/admin/AirlineForm";
import { AirportForm } from "../components/admin/AirportForm";
import { FlightForm } from "../components/admin/FlightForm";
// Airlines are deactivated rather than deleted here: a carrier with flights
// cannot be removed, and taking it out of service is what an operator actually
// wants. `deleteAirline` exists in the API module for completeness.
import {
  getAllAirlines,
  deactivateAirline,
  reactivateAirline,
} from "../api/airlines";
import { getAllAirports, deleteAirport } from "../api/airports";
import { getFlightsPage, deleteFlight } from "../api/flights";
import { getBookingsByStatus } from "../api/bookings";
import { useCurrency } from "../lib/currency";
import { formatLocalDateTime } from "../lib/datetime";
import type { AirlineResponseDto } from "../types/airline";
import type { AirportResponseDto, FlightResponseDto } from "../types/flight";
import type { BookingResponseDto } from "../types/booking";
import type { PagedResponseDto } from "../types/paging";

type LoadState = "loading" | "error" | "ready";
type Tab = "bookings" | "flights" | "airlines" | "airports";

const TABS: { id: Tab; label: string }[] = [
  { id: "bookings", label: "Bookings" },
  { id: "flights", label: "Flights" },
  { id: "airlines", label: "Airlines" },
  { id: "airports", label: "Airports" },
];

const BOOKING_STATUSES = ["CONFIRMED", "PENDING", "CANCELLED", "COMPLETED"] as const;
type BookingStatus = (typeof BOOKING_STATUSES)[number];

const STATUS_OPTIONS: SelectOption<BookingStatus>[] = BOOKING_STATUSES.map((s) => ({
  value: s,
  label: s.charAt(0) + s.slice(1).toLowerCase(),
}));

/** The flights table is thousands of rows; only ever render a readable slice. */
const FLIGHT_PAGE_SIZE = 40;

const CARD =
  "rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8";
const ROW =
  "flex items-center justify-between gap-4 rounded-xl border border-zinc-200 px-4 py-3 dark:border-white/10";
const ICON_BUTTON =
  "flex h-8 w-8 cursor-pointer items-center justify-center rounded-lg border border-zinc-200 text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-900 disabled:opacity-40 dark:border-white/10 dark:text-zinc-400 dark:hover:bg-white/5 dark:hover:text-zinc-100";

function SearchBox({
  value,
  onChange,
  placeholder,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder: string;
}) {
  return (
    <label className="flex min-w-0 flex-1 items-center gap-2.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30 sm:max-w-xs">
      <Search className="h-4 w-4 shrink-0 text-zinc-300 dark:text-zinc-600" strokeWidth={2} />
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full min-w-0 bg-transparent text-sm font-medium text-zinc-900 outline-none placeholder:text-zinc-300 dark:text-zinc-100 dark:placeholder:text-zinc-700"
      />
    </label>
  );
}

export function AdminPage() {
  const { formatPrice } = useCurrency();
  const [tab, setTab] = useState<Tab>("bookings");

  const [reference, setReference] = useState<{
    airlines: AirlineResponseDto[];
    airports: AirportResponseDto[];
  } | null>(null);
  const [referenceState, setReferenceState] = useState<LoadState>("loading");

  const [bookingStatus, setBookingStatus] = useState<BookingStatus>("CONFIRMED");
  const [bookingsLoad, setBookingsLoad] = useState<{
    status: BookingStatus;
    bookings: BookingResponseDto[] | "error";
  } | null>(null);

  /** What the admin is typing. */
  const [flightQuery, setFlightQuery] = useState("");
  /** What has actually been sent to the server — see the debounce below. */
  const [flightSearch, setFlightSearch] = useState("");
  const [flightPage, setFlightPage] = useState(0);
  /** Bumped after a create, edit or delete to force the page to be re-read. */
  const [flightRevision, setFlightRevision] = useState(0);
  const [flightLoad, setFlightLoad] = useState<{
    key: string;
    data: PagedResponseDto<FlightResponseDto> | "error";
  } | null>(null);

  const [airportQuery, setAirportQuery] = useState("");

  /** Which record the form modal is editing; `"new"` means create. */
  const [editingAirline, setEditingAirline] = useState<AirlineResponseDto | "new" | null>(null);
  const [editingAirport, setEditingAirport] = useState<AirportResponseDto | "new" | null>(null);
  const [editingFlight, setEditingFlight] = useState<FlightResponseDto | "new" | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([getAllAirlines(), getAllAirports()])
      .then(([airlines, airports]) => {
        if (cancelled) return;
        setReference({ airlines, airports });
        setReferenceState("ready");
      })
      .catch(() => {
        if (!cancelled) setReferenceState("error");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    getBookingsByStatus(bookingStatus)
      .then((data) => {
        if (!cancelled) setBookingsLoad({ status: bookingStatus, bookings: data });
      })
      .catch(() => {
        if (!cancelled) setBookingsLoad({ status: bookingStatus, bookings: "error" });
      });
    return () => {
      cancelled = true;
    };
  }, [bookingStatus]);

  // Typing shouldn't fire a query per keystroke. Landing on the first page as
  // the term changes matters too: page 7 of "DUB" is rarely page 7 of "DUBL".
  useEffect(() => {
    const timer = setTimeout(() => {
      setFlightSearch(flightQuery.trim());
      setFlightPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [flightQuery]);

  const flightKey = `${flightSearch}::${flightPage}::${flightRevision}`;

  // Flights are only fetched once the tab is opened — it is the heaviest list
  // in the app and most admin visits never look at it.
  useEffect(() => {
    if (tab !== "flights") return;
    let cancelled = false;

    getFlightsPage(flightSearch, flightPage, FLIGHT_PAGE_SIZE)
      .then((data) => {
        if (!cancelled) setFlightLoad({ key: flightKey, data });
      })
      .catch(() => {
        if (!cancelled) setFlightLoad({ key: flightKey, data: "error" });
      });

    return () => {
      cancelled = true;
    };
  }, [tab, flightSearch, flightPage, flightKey]);

  const airlines = reference?.airlines ?? [];
  const airports = reference?.airports ?? [];

  const currentBookings = bookingsLoad?.status === bookingStatus ? bookingsLoad : null;
  const bookingsState: LoadState = !currentBookings
    ? "loading"
    : currentBookings.bookings === "error"
      ? "error"
      : "ready";
  const bookings =
    currentBookings && currentBookings.bookings !== "error" ? currentBookings.bookings : [];

  const currentFlights = flightLoad?.key === flightKey ? flightLoad : null;
  const flightsState: LoadState = !currentFlights
    ? "loading"
    : currentFlights.data === "error"
      ? "error"
      : "ready";
  const flightPageData =
    currentFlights && currentFlights.data !== "error" ? currentFlights.data : null;
  const visibleFlights = flightPageData?.content ?? [];

  const airportTerm = airportQuery.trim().toLowerCase();
  const visibleAirports = airportTerm
    ? airports.filter(
        (a) =>
          a.code.toLowerCase().includes(airportTerm) ||
          a.city.toLowerCase().includes(airportTerm) ||
          a.country.toLowerCase().includes(airportTerm),
      )
    : airports;

  const replaceAirline = (saved: AirlineResponseDto) =>
    setReference((prev) =>
      prev
        ? {
            ...prev,
            airlines: prev.airlines.some((a) => a.id === saved.id)
              ? prev.airlines.map((a) => (a.id === saved.id ? saved : a))
              : [...prev.airlines, saved],
          }
        : prev,
    );

  const replaceAirport = (saved: AirportResponseDto) =>
    setReference((prev) =>
      prev
        ? {
            ...prev,
            airports: prev.airports.some((a) => a.id === saved.id)
              ? prev.airports.map((a) => (a.id === saved.id ? saved : a))
              : [...prev.airports, saved],
          }
        : prev,
    );

  const toggleAirline = async (airline: AirlineResponseDto) => {
    setBusyId(airline.id);
    try {
      if (airline.active) await deactivateAirline(airline.id);
      else await reactivateAirline(airline.id);
      replaceAirline({ ...airline, active: !airline.active });
    } catch {
      // Row is left as it was; the list re-reads on next visit.
    } finally {
      setBusyId(null);
    }
  };

  /**
   * Deletes are refused by the API when something still references the record.
   * That rejection is the useful bit, so it is surfaced rather than swallowed.
   */
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const removeRecord = async (
    id: number,
    remove: (id: number) => Promise<void>,
    onDone: () => void,
  ) => {
    setBusyId(id);
    setDeleteError(null);
    try {
      await remove(id);
      onDone();
    } catch {
      setDeleteError(
        "Could not delete — records that are still referenced by flights or bookings cannot be removed. Deactivate instead.",
      );
    } finally {
      setBusyId(null);
    }
  };

  return (
    <main className="mx-auto mt-10 max-w-5xl">
      <PageHeader
        eyebrow="Operations"
        title="Admin console"
        subtitle="Network-wide bookings, flights and reference data. Every action here is restricted to admin accounts by the API, not just by this page."
      />

      {referenceState === "error" && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
          Could not load admin data.
        </p>
      )}

      {referenceState === "ready" && (
        <>
          <div className="mb-6 flex gap-1 overflow-x-auto rounded-xl border border-zinc-200 p-1 dark:border-white/10">
            {TABS.map((t) => (
              <button
                key={t.id}
                type="button"
                onClick={() => setTab(t.id)}
                className={`flex-1 cursor-pointer whitespace-nowrap rounded-lg px-4 py-1.5 text-sm font-medium transition-colors ${
                  tab === t.id
                    ? "bg-zinc-900 text-white dark:bg-white dark:text-zinc-900"
                    : "text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>

          {deleteError && (
            <p className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
              {deleteError}
            </p>
          )}

          {/* ------------------------------------------------ bookings -- */}
          {tab === "bookings" && (
            <section className={CARD}>
              <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <h2 className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
                    All bookings
                  </h2>
                  <p className="mt-0.5 text-sm text-zinc-500 dark:text-zinc-400">
                    Across every customer — this view is admin-only.
                  </p>
                </div>
                <div className="sm:w-48">
                  <SelectField
                    label="Status"
                    value={bookingStatus}
                    options={STATUS_OPTIONS}
                    onChange={setBookingStatus}
                  />
                </div>
              </div>

              {bookingsState === "loading" && (
                <div className="flex items-center gap-3 py-6 text-sm text-zinc-500 dark:text-zinc-400">
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
                  Loading bookings…
                </div>
              )}

              {bookingsState === "ready" && bookings.length === 0 && (
                <p className="rounded-xl border border-dashed border-zinc-200 px-4 py-8 text-center text-sm text-zinc-500 dark:border-white/10 dark:text-zinc-400">
                  No {bookingStatus.toLowerCase()} bookings.
                </p>
              )}

              {bookingsState === "ready" && bookings.length > 0 && (
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[42rem] text-left text-sm">
                    <thead>
                      <tr className="border-b border-zinc-200 text-[11px] uppercase tracking-wider text-zinc-400 dark:border-white/10 dark:text-zinc-500">
                        <th className="pb-2 font-medium">Reference</th>
                        <th className="pb-2 font-medium">Customer</th>
                        <th className="pb-2 font-medium">Route</th>
                        <th className="pb-2 font-medium">Departs</th>
                        <th className="pb-2 text-right font-medium">Amount</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-100 dark:divide-white/5">
                      {bookings.map((b) => (
                        <tr key={b.id}>
                          <td className="py-3 font-mono text-xs text-zinc-900 dark:text-zinc-100">
                            {b.bookingReference}
                          </td>
                          <td className="py-3 text-zinc-600 dark:text-zinc-300">
                            {b.userEmail}
                          </td>
                          <td className="py-3 font-mono text-xs text-zinc-600 dark:text-zinc-300">
                            {b.departureAirport} → {b.arrivalAirport}
                          </td>
                          <td className="py-3 text-zinc-500 dark:text-zinc-400">
                            {formatLocalDateTime(b.departureTime, b.departureTimezone)}
                          </td>
                          <td className="py-3 text-right font-medium text-zinc-900 dark:text-zinc-100">
                            {formatPrice(b.totalAmount)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          {/* ------------------------------------------------- flights -- */}
          {tab === "flights" && (
            <section className={CARD}>
              <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <SearchBox
                  value={flightQuery}
                  onChange={setFlightQuery}
                  placeholder="Flight number, route or airline"
                />
                <button
                  type="button"
                  onClick={() => setEditingFlight("new")}
                  className="flex cursor-pointer items-center justify-center gap-1.5 rounded-xl bg-zinc-900 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
                >
                  <Plus className="h-4 w-4" strokeWidth={2.5} />
                  New flight
                </button>
              </div>

              {flightsState === "loading" && (
                <div className="flex items-center gap-3 py-6 text-sm text-zinc-500 dark:text-zinc-400">
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
                  Loading flights…
                </div>
              )}

              {flightsState === "error" && (
                <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
                  Could not load flights.
                </p>
              )}

              {flightPageData && visibleFlights.length === 0 && (
                <p className="rounded-xl border border-dashed border-zinc-200 px-4 py-8 text-center text-sm text-zinc-500 dark:border-white/10 dark:text-zinc-400">
                  No flights match “{flightSearch}”.
                </p>
              )}

              {flightPageData && visibleFlights.length > 0 && (
                <>
                  <p className="mb-3 text-xs text-zinc-400 dark:text-zinc-500">
                    {flightPageData.totalElements.toLocaleString()} matching flight
                    {flightPageData.totalElements === 1 ? "" : "s"} · page{" "}
                    {flightPageData.page + 1} of {flightPageData.totalPages}
                  </p>
                  <div className="flex flex-col gap-2">
                    {visibleFlights.map((f) => (
                      <div key={f.id} className={ROW}>
                        <div className="flex min-w-0 items-center gap-3">
                          <span className="w-16 shrink-0 font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                            {f.flightNumber}
                          </span>
                          <div className="min-w-0">
                            <p className="truncate font-mono text-sm text-zinc-900 dark:text-zinc-100">
                              {f.departureAirportCode} → {f.arrivalAirportCode}
                            </p>
                            <p className="truncate text-xs text-zinc-400 dark:text-zinc-500">
                              {f.airlineName} ·{" "}
                              {formatLocalDateTime(
                                f.departureTime,
                                airports.find((a) => a.code === f.departureAirportCode)
                                  ?.timezone ?? null,
                              )}
                            </p>
                          </div>
                        </div>
                        <div className="flex shrink-0 items-center gap-2">
                          <button
                            type="button"
                            aria-label={`Edit ${f.flightNumber}`}
                            onClick={() => setEditingFlight(f)}
                            className={ICON_BUTTON}
                          >
                            <Pencil className="h-3.5 w-3.5" strokeWidth={2} />
                          </button>
                          <button
                            type="button"
                            aria-label={`Delete ${f.flightNumber}`}
                            disabled={busyId === f.id}
                            onClick={() =>
                              removeRecord(f.id, deleteFlight, () =>
                                setFlightRevision((r) => r + 1),
                              )
                            }
                            className={ICON_BUTTON}
                          >
                            <Trash2 className="h-3.5 w-3.5" strokeWidth={2} />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>

                  {flightPageData.totalPages > 1 && (
                    <div className="mt-5 flex items-center justify-between gap-3 border-t border-zinc-100 pt-4 dark:border-white/5">
                      <button
                        type="button"
                        onClick={() => setFlightPage((p) => Math.max(p - 1, 0))}
                        disabled={!flightPageData.hasPrevious}
                        className="flex cursor-pointer items-center gap-1.5 rounded-lg border border-zinc-200 px-3 py-1.5 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 disabled:cursor-not-allowed disabled:opacity-40 dark:border-white/10 dark:text-zinc-300 dark:hover:bg-white/5"
                      >
                        <ChevronLeft className="h-4 w-4" strokeWidth={2} />
                        Previous
                      </button>
                      <span className="text-xs tabular-nums text-zinc-400 dark:text-zinc-500">
                        {flightPageData.page * flightPageData.size + 1}–
                        {flightPageData.page * flightPageData.size + visibleFlights.length}
                        {" of "}
                        {flightPageData.totalElements.toLocaleString()}
                      </span>
                      <button
                        type="button"
                        onClick={() => setFlightPage((p) => p + 1)}
                        disabled={!flightPageData.hasNext}
                        className="flex cursor-pointer items-center gap-1.5 rounded-lg border border-zinc-200 px-3 py-1.5 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 disabled:cursor-not-allowed disabled:opacity-40 dark:border-white/10 dark:text-zinc-300 dark:hover:bg-white/5"
                      >
                        Next
                        <ChevronRight className="h-4 w-4" strokeWidth={2} />
                      </button>
                    </div>
                  )}
                </>
              )}
            </section>
          )}

          {/* ------------------------------------------------ airlines -- */}
          {tab === "airlines" && (
            <section className={CARD}>
              <div className="mb-5 flex items-center justify-between gap-3">
                <div>
                  <h2 className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
                    Airlines
                  </h2>
                  <p className="mt-0.5 text-sm text-zinc-500 dark:text-zinc-400">
                    Deactivating takes a carrier out of service without deleting
                    its booking history.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setEditingAirline("new")}
                  className="flex shrink-0 cursor-pointer items-center gap-1.5 rounded-xl bg-zinc-900 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
                >
                  <Plus className="h-4 w-4" strokeWidth={2.5} />
                  New
                </button>
              </div>

              <div className="flex flex-col gap-2">
                {airlines.map((airline) => (
                  <motion.div layout key={airline.id} className={ROW}>
                    <div className="flex min-w-0 items-center gap-3">
                      <span className="w-8 shrink-0 font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                        {airline.code}
                      </span>
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-zinc-900 dark:text-zinc-100">
                          {airline.name}
                        </p>
                        <p className="truncate text-xs text-zinc-400 dark:text-zinc-500">
                          {airline.country}
                        </p>
                      </div>
                    </div>

                    <div className="flex shrink-0 items-center gap-2">
                      <span
                        className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${
                          airline.active
                            ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-400"
                            : "bg-red-100 text-red-700 dark:bg-red-400/10 dark:text-red-400"
                        }`}
                      >
                        {airline.active ? (
                          <CheckCircle2 className="h-3 w-3" strokeWidth={2.5} />
                        ) : (
                          <Ban className="h-3 w-3" strokeWidth={2.5} />
                        )}
                        {airline.active ? "Active" : "Inactive"}
                      </span>
                      <button
                        type="button"
                        onClick={() => toggleAirline(airline)}
                        disabled={busyId === airline.id}
                        className="cursor-pointer rounded-lg border border-zinc-200 px-3 py-1.5 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 disabled:opacity-60 dark:border-white/10 dark:text-zinc-300 dark:hover:bg-white/5"
                      >
                        {airline.active ? "Deactivate" : "Reactivate"}
                      </button>
                      <button
                        type="button"
                        aria-label={`Edit ${airline.code}`}
                        onClick={() => setEditingAirline(airline)}
                        className={ICON_BUTTON}
                      >
                        <Pencil className="h-3.5 w-3.5" strokeWidth={2} />
                      </button>
                    </div>
                  </motion.div>
                ))}
              </div>
            </section>
          )}

          {/* ------------------------------------------------ airports -- */}
          {tab === "airports" && (
            <section className={CARD}>
              <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <SearchBox
                  value={airportQuery}
                  onChange={setAirportQuery}
                  placeholder="Code, city or country"
                />
                <button
                  type="button"
                  onClick={() => setEditingAirport("new")}
                  className="flex cursor-pointer items-center justify-center gap-1.5 rounded-xl bg-zinc-900 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
                >
                  <Plus className="h-4 w-4" strokeWidth={2.5} />
                  New airport
                </button>
              </div>

              <div className="flex flex-col gap-2">
                {visibleAirports.map((airport) => (
                  <motion.div layout key={airport.id} className={ROW}>
                    <div className="flex min-w-0 items-center gap-3">
                      <span className="w-10 shrink-0 font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                        {airport.code}
                      </span>
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-zinc-900 dark:text-zinc-100">
                          {airport.city}
                        </p>
                        <p className="truncate text-xs text-zinc-400 dark:text-zinc-500">
                          {airport.country}
                          {airport.latitude == null && " · no coordinates"}
                        </p>
                      </div>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <button
                        type="button"
                        aria-label={`Edit ${airport.code}`}
                        onClick={() => setEditingAirport(airport)}
                        className={ICON_BUTTON}
                      >
                        <Pencil className="h-3.5 w-3.5" strokeWidth={2} />
                      </button>
                      <button
                        type="button"
                        aria-label={`Delete ${airport.code}`}
                        disabled={busyId === airport.id}
                        onClick={() =>
                          removeRecord(airport.id, deleteAirport, () =>
                            setReference((prev) =>
                              prev
                                ? {
                                    ...prev,
                                    airports: prev.airports.filter(
                                      (a) => a.id !== airport.id,
                                    ),
                                  }
                                : prev,
                            ),
                          )
                        }
                        className={ICON_BUTTON}
                      >
                        <Trash2 className="h-3.5 w-3.5" strokeWidth={2} />
                      </button>
                    </div>
                  </motion.div>
                ))}
              </div>
            </section>
          )}
        </>
      )}

      {editingAirline && (
        <AirlineForm
          airline={editingAirline === "new" ? undefined : editingAirline}
          onSaved={replaceAirline}
          onClose={() => setEditingAirline(null)}
        />
      )}

      {editingAirport && (
        <AirportForm
          airport={editingAirport === "new" ? undefined : editingAirport}
          onSaved={replaceAirport}
          onClose={() => setEditingAirport(null)}
        />
      )}

      {editingFlight && (
        <FlightForm
          flight={editingFlight === "new" ? undefined : editingFlight}
          airlines={airlines}
          airports={airports}
          // The page is server-owned now, so a save is reflected by re-reading
          // it rather than splicing the row in locally — a new flight may not
          // even belong on the page currently in view.
          onSaved={() => setFlightRevision((r) => r + 1)}
          onClose={() => setEditingFlight(null)}
        />
      )}
    </main>
  );
}
