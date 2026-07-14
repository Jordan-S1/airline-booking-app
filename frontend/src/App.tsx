import { useState } from "react";
import axios from "axios";
import { motion } from "framer-motion";
import { Navbar } from "./components/Navbar";
import { FlightSearchPanel } from "./components/FlightSearchPanel";
import { FlightStatusWidget } from "./components/FlightStatusWidget";
import { WeatherWidget } from "./components/WeatherWidget";
import { LoyaltyWidget } from "./components/LoyaltyWidget";
import { FlightResultsList } from "./components/FlightResultsList";
import { BookingModal } from "./components/BookingModal";
import { mockFlightStatus, mockLoyalty } from "./data/mockFlight";
import { searchFlights } from "./api/flights";
import type {
  FlightSearchRequestDto,
  FlightSearchResponseDto,
} from "./types/flight";

type SearchStatus = "idle" | "loading" | "error" | "success";

function App() {
  const [searchStatus, setSearchStatus] = useState<SearchStatus>("idle");
  const [searchError, setSearchError] = useState<string | null>(null);
  const [outboundFlights, setOutboundFlights] = useState<
    FlightSearchResponseDto[]
  >([]);
  const [lastSearchRequest, setLastSearchRequest] =
    useState<FlightSearchRequestDto | null>(null);
  const [selectedFlight, setSelectedFlight] =
    useState<FlightSearchResponseDto | null>(null);

  const handleSearch = async (request: FlightSearchRequestDto) => {
    setSearchStatus("loading");
    setSearchError(null);

    try {
      const result = await searchFlights(request);
      setOutboundFlights(result.outboundFlights);
      setLastSearchRequest(request);
      setSearchStatus("success");
    } catch (err) {
      const message = axios.isAxiosError(err)
        ? (err.response?.data?.message ?? err.message)
        : "Unable to reach the flight search service.";
      setSearchError(message);
      setSearchStatus("error");
    }
  };

  return (
    <div className="relative min-h-screen overflow-x-hidden px-4 pb-20 sm:px-6 lg:px-8">
      {/* Ambient background glow */}
      <div
        aria-hidden
        className="pointer-events-none fixed inset-0 -z-10 bg-[radial-gradient(circle_at_20%_-10%,rgba(6,182,212,0.10),transparent_45%),radial-gradient(circle_at_85%_10%,rgba(6,182,212,0.06),transparent_40%)] dark:bg-[radial-gradient(circle_at_20%_-10%,rgba(6,182,212,0.14),transparent_45%),radial-gradient(circle_at_85%_10%,rgba(6,182,212,0.08),transparent_40%)]"
      />

      <Navbar />

      <main className="mx-auto mt-10 max-w-6xl">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="mb-8"
        >
          <p className="text-sm font-medium text-accent">
            Good afternoon, Jordan
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100 sm:text-4xl">
            Flight control dashboard
          </h1>
          <p className="mt-2 max-w-xl text-sm text-zinc-500 dark:text-zinc-400">
            Track your next departure and book new itineraries across the
            SkyAir network in one place.
          </p>
        </motion.div>

        <motion.div
          initial="hidden"
          animate="show"
          variants={{
            hidden: {},
            show: { transition: { staggerChildren: 0.08 } },
          }}
          className="grid grid-cols-1 gap-5 lg:grid-cols-3 lg:grid-rows-2"
        >
          <motion.div
            variants={{
              hidden: { opacity: 0, y: 16 },
              show: { opacity: 1, y: 0 },
            }}
            className="lg:col-span-1 lg:row-span-2"
          >
            <FlightSearchPanel
              onSearch={handleSearch}
              isSearching={searchStatus === "loading"}
            />
          </motion.div>

          <motion.div
            variants={{
              hidden: { opacity: 0, y: 16 },
              show: { opacity: 1, y: 0 },
            }}
            className="lg:col-span-2"
          >
            <FlightStatusWidget flight={mockFlightStatus} />
          </motion.div>

          <motion.div
            variants={{
              hidden: { opacity: 0, y: 16 },
              show: { opacity: 1, y: 0 },
            }}
            className="lg:col-span-1"
          >
            <WeatherWidget
              airportCode={mockFlightStatus.destination.code}
              city={mockFlightStatus.destination.city}
            />
          </motion.div>

          <motion.div
            variants={{
              hidden: { opacity: 0, y: 16 },
              show: { opacity: 1, y: 0 },
            }}
            className="lg:col-span-1"
          >
            <LoyaltyWidget loyalty={mockLoyalty} />
          </motion.div>
        </motion.div>

        <FlightResultsList
          status={searchStatus}
          errorMessage={searchError}
          outboundFlights={outboundFlights}
          onSelectFlight={setSelectedFlight}
        />
      </main>

      {selectedFlight && lastSearchRequest && (
        <BookingModal
          flight={selectedFlight}
          passengerCount={lastSearchRequest.passengers}
          seatClass={lastSearchRequest.seatClass}
          onClose={() => setSelectedFlight(null)}
        />
      )}
    </div>
  );
}

export default App;
