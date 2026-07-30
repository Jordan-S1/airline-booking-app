import { useCallback, useEffect, useRef, useState } from "react";
import axios from "axios";
import { motion } from "framer-motion";
import { FlightSearchPanel } from "../components/FlightSearchPanel";
import { FlightStatusWidget } from "../components/FlightStatusWidget";
import { WeatherWidget } from "../components/WeatherWidget";
import { TravelStatsWidget } from "../components/TravelStatsWidget";
import {
  FlightResultsList,
  type ResultSection,
} from "../components/FlightResultsList";
import { BookingModal } from "../components/BookingModal";
import { AuthModal } from "../components/AuthModal";
import type { SearchSubmission } from "../components/FlightSearchPanel";
import {
  searchFlights,
  searchMultiCity,
  getFlightStatus,
} from "../api/flights";
import { getBookingsByUser } from "../api/bookings";
import { useAuth } from "../lib/auth";
import { useCurrency } from "../lib/currency";
import type {
  FlightSearchResponseDto,
  FlightStatusDto,
} from "../types/flight";
import type { BookingResponseDto } from "../types/booking";

type SearchStatus = "idle" | "loading" | "error" | "success";
type TrackedStatus = "loading" | "error" | "empty" | "ready";
type BookingsStatus = "loading" | "error" | "ready";

/** The tracked flight, or where its lookup got to. `null` means none was found. */
type TrackedResult = "loading" | "error" | FlightStatusDto | null;

/**
 * The dashboard's per-user data, tagged with the user it was loaded for.
 * Everything the widgets read is derived from this, so switching user makes the
 * UI fall back to "loading" on its own rather than an effect resetting it.
 */
interface DashboardLoad {
  userId: number;
  bookings: BookingResponseDto[] | "error";
  tracked: TrackedResult;
}

/** A leg of the current search, with whichever flight the user picked for it. */
interface SearchLeg {
  id: string;
  title: string;
  subtitle: string;
  flights: FlightSearchResponseDto[];
  selected: FlightSearchResponseDto | null;
}

function formatLegDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", {
    day: "numeric",
    month: "short",
  });
}

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}

export function DashboardPage() {
  const { user } = useAuth();
  const { formatPrice } = useCurrency();
  const searchPanelRef = useRef<HTMLDivElement>(null);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [searchStatus, setSearchStatus] = useState<SearchStatus>("idle");
  const [searchError, setSearchError] = useState<string | null>(null);
  const [searchLegs, setSearchLegs] = useState<SearchLeg[]>([]);
  const [passengerCount, setPassengerCount] = useState(1);
  const [searchSeatClass, setSearchSeatClass] = useState<string>("ECONOMY");
  const [bookingFlights, setBookingFlights] = useState<
    FlightSearchResponseDto[] | null
  >(null);
  // Bookings are fetched once here and shared by the status widget and the
  // travel stats card.
  const [load, setLoad] = useState<DashboardLoad | null>(null);

  // Track the signed-in user's most relevant booking: the next flight that
  // hasn't landed yet, falling back to their most recent one.
  useEffect(() => {
    if (!user) return;

    const userId = user.userId;
    let cancelled = false;

    (async () => {
      let userBookings: BookingResponseDto[];
      try {
        userBookings = await getBookingsByUser(userId);
      } catch {
        if (!cancelled)
          setLoad({ userId, bookings: "error", tracked: "error" });
        return;
      }
      if (cancelled) return;

      const active = userBookings.filter((b) => b.status !== "CANCELLED");
      if (active.length === 0) {
        setLoad({ userId, bookings: userBookings, tracked: null });
        return;
      }

      // Show the bookings straight away; the flight status is a second hop.
      setLoad({ userId, bookings: userBookings, tracked: "loading" });

      const now = Date.now();
      const upcoming = active
        .filter((b) => new Date(b.arrivalTime).getTime() >= now)
        .sort(
          (a, b) =>
            new Date(a.departureTime).getTime() -
            new Date(b.departureTime).getTime(),
        );
      const mostRecent = [...active].sort(
        (a, b) =>
          new Date(b.departureTime).getTime() -
          new Date(a.departureTime).getTime(),
      );
      const target = upcoming[0] ?? mostRecent[0];

      try {
        const status = await getFlightStatus(target.flightId);
        if (!cancelled)
          setLoad({ userId, bookings: userBookings, tracked: status });
      } catch {
        if (!cancelled)
          setLoad({ userId, bookings: userBookings, tracked: "error" });
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [user]);

  // Anything not tagged with the current user is stale, which covers both the
  // signed-out case and the gap right after switching accounts.
  const current = user && load?.userId === user.userId ? load : null;

  const bookingsStatus: BookingsStatus = !user
    ? "ready"
    : !current
      ? "loading"
      : current.bookings === "error"
        ? "error"
        : "ready";
  const bookings = current && current.bookings !== "error" ? current.bookings : [];

  const trackedStatus: TrackedStatus = !user
    ? "empty"
    : !current || current.tracked === "loading"
      ? "loading"
      : current.tracked === "error"
        ? "error"
        : current.tracked === null
          ? "empty"
          : "ready";
  const trackedFlight =
    current &&
    current.tracked !== null &&
    current.tracked !== "loading" &&
    current.tracked !== "error"
      ? current.tracked
      : null;

  /**
   * The search panel already lives on this page, so the empty-state CTA scrolls
   * to it rather than navigating. On mobile the panel sits above the fold of the
   * status widget, so this is a real jump; on desktop it just draws the eye.
   */
  const focusSearchPanel = useCallback(() => {
    const panel = searchPanelRef.current;
    if (!panel) return;
    // Multi-city grows the panel past the viewport once it has a few legs, and
    // centring something taller than the screen pushes its submit button out of
    // sight. In that case scroll to the button itself with "nearest", which
    // moves the minimum distance needed to reveal it rather than running the
    // page all the way down.
    const fitsOnScreen =
      panel.getBoundingClientRect().height <= window.innerHeight;
    const submit = panel.querySelector<HTMLElement>("[data-search-submit]");

    if (fitsOnScreen || !submit) {
      panel.scrollIntoView({ behavior: "smooth", block: "center" });
    } else {
      submit.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    panel.focus({ preventScroll: true });
  }, []);

  const handleSearch = async ({ tripType, simple, legs }: SearchSubmission) => {
    setSearchStatus("loading");
    setSearchError(null);
    setSearchLegs([]);
    setPassengerCount(simple.passengers);
    setSearchSeatClass(simple.seatClass);

    try {
      if (tripType === "MULTI_CITY") {
        const result = await searchMultiCity({
          legs,
          passengers: simple.passengers,
          seatClass: simple.seatClass,
          directFlightsOnly: simple.directFlightsOnly,
        });

        setSearchLegs(
          result.legs.map((leg) => ({
            id: `leg-${leg.legNumber}`,
            title: `Leg ${leg.legNumber}: ${leg.departureAirport} → ${leg.arrivalAirport}`,
            subtitle: formatLegDate(leg.departureDate),
            flights: leg.flights,
            selected: null,
          })),
        );
      } else {
        // A returnDate is what makes the backend treat this as a round trip.
        const request = {
          ...simple,
          returnDate: tripType === "ROUND_TRIP" ? simple.returnDate : null,
        };
        const result = await searchFlights(request);

        const nextLegs: SearchLeg[] = [
          {
            id: "outbound",
            title: `Outbound: ${simple.departureAirport} → ${simple.arrivalAirport}`,
            subtitle: formatLegDate(simple.departureDate),
            flights: result.outboundFlights,
            selected: null,
          },
        ];

        if (result.isRoundTrip && request.returnDate) {
          nextLegs.push({
            id: "return",
            title: `Return: ${simple.arrivalAirport} → ${simple.departureAirport}`,
            subtitle: formatLegDate(request.returnDate),
            flights: result.returnFlights ?? [],
            selected: null,
          });
        }

        setSearchLegs(nextLegs);
      }

      setSearchStatus("success");
    } catch (err) {
      const message = axios.isAxiosError(err)
        ? (err.response?.data?.message ?? err.message)
        : "Unable to reach the flight search service.";
      setSearchError(message);
      setSearchStatus("error");
    }
  };

  const handleSelectFlight = (
    sectionId: string,
    flight: FlightSearchResponseDto,
  ) => {
    // A single-leg search books immediately; multi-leg trips collect a
    // selection per leg and book the whole itinerary at the end.
    if (searchLegs.length === 1) {
      setBookingFlights([flight]);
      return;
    }

    setSearchLegs((prev) =>
      prev.map((leg) =>
        leg.id === sectionId
          ? { ...leg, selected: leg.selected?.id === flight.id ? null : flight }
          : leg,
      ),
    );
  };

  const sections: ResultSection[] = searchLegs.map((leg) => ({
    id: leg.id,
    title: leg.title,
    subtitle: leg.subtitle,
    flights: leg.flights,
    selectedFlightId: leg.selected?.id ?? null,
  }));

  const isMultiLeg = searchLegs.length > 1;
  const selectedLegFlights = searchLegs
    .map((leg) => leg.selected)
    .filter((f): f is FlightSearchResponseDto => f !== null);
  const allLegsSelected =
    isMultiLeg && selectedLegFlights.length === searchLegs.length;
  const itineraryTotal = selectedLegFlights.reduce(
    (sum, f) => sum + f.price,
    0,
  );

  return (
    <main className="mx-auto mt-10 max-w-6xl">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
        className="mb-8"
      >
        <p className="text-sm font-medium text-accent">
          {greeting()}
          {user ? `, ${user.firstName}` : ""}
        </p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100 sm:text-4xl">
          Flight control dashboard
        </h1>
        <p className="mt-2 max-w-xl text-sm text-zinc-500 dark:text-zinc-400">
          Track your next departure and book new itineraries across the SkyAir
          network in one place.
        </p>
      </motion.div>

      <motion.div
        initial="hidden"
        animate="show"
        variants={{
          hidden: {},
          show: { transition: { staggerChildren: 0.08 } },
        }}
        className="grid grid-cols-1 gap-5 lg:grid-cols-3"
      >
        <motion.div
          variants={{
            hidden: { opacity: 0, y: 16 },
            show: { opacity: 1, y: 0 },
          }}
          className="lg:col-span-1 lg:row-span-2"
        >
          {/* tabIndex allows focusSearchPanel to move keyboard focus here, not
              just scroll — scrolling alone leaves focus stranded further down. */}
          <div ref={searchPanelRef} tabIndex={-1} className="outline-none">
            <FlightSearchPanel
              onSearch={handleSearch}
              isSearching={searchStatus === "loading"}
            />
          </div>
        </motion.div>

        <motion.div
          variants={{
            hidden: { opacity: 0, y: 16 },
            show: { opacity: 1, y: 0 },
          }}
          className="lg:col-span-2"
        >
          <FlightStatusWidget
            status={trackedStatus}
            flight={trackedFlight}
            isAuthenticated={user !== null}
            onFindFlights={focusSearchPanel}
            onSignIn={() => setIsAuthModalOpen(true)}
          />
        </motion.div>

        <motion.div
          variants={{
            hidden: { opacity: 0, y: 16 },
            show: { opacity: 1, y: 0 },
          }}
          className="lg:col-span-1"
        >
          {/* Follows the tracked flight's destination; falls back to a hub
              airport when there is nothing to track. */}
          <WeatherWidget
            airportCode={trackedFlight?.arrivalAirport ?? "DUB"}
            city={trackedFlight?.arrivalCity ?? "Dublin"}
          />
        </motion.div>

        <motion.div
          variants={{
            hidden: { opacity: 0, y: 16 },
            show: { opacity: 1, y: 0 },
          }}
          className="lg:col-span-1"
        >
          <TravelStatsWidget
            status={bookingsStatus}
            bookings={bookings}
            isAuthenticated={user !== null}
          />
        </motion.div>

      </motion.div>

      <FlightResultsList
        status={searchStatus}
        errorMessage={searchError}
        sections={sections}
        onSelectFlight={handleSelectFlight}
        footer={
          isMultiLeg ? (
            <div className="sticky bottom-4 flex flex-wrap items-center justify-between gap-4 rounded-xl border border-zinc-200 bg-white/80 px-5 py-4 shadow-sm backdrop-blur-md dark:border-white/10 dark:bg-white/5">
              <div>
                <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
                  {selectedLegFlights.length} of {searchLegs.length} legs
                  selected
                </p>
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                  {allLegsSelected
                    ? `Itinerary total ${formatPrice(itineraryTotal * passengerCount)}`
                    : "Pick a flight for each leg to continue."}
                </p>
              </div>
              <button
                type="button"
                disabled={!allLegsSelected}
                onClick={() => setBookingFlights(selectedLegFlights)}
                className="cursor-pointer rounded-xl bg-zinc-900 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
              >
                Book itinerary
              </button>
            </div>
          ) : undefined
        }
      />

      {bookingFlights && (
        <BookingModal
          flights={bookingFlights}
          passengerCount={passengerCount}
          seatClass={searchSeatClass}
          onClose={() => setBookingFlights(null)}
        />
      )}

      {isAuthModalOpen && (
        <AuthModal onClose={() => setIsAuthModalOpen(false)} />
      )}
    </main>
  );
}
