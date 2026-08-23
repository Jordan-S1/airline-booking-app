import { useEffect, useRef, useState } from "react";
import axios from "axios";
import { motion } from "framer-motion";
import { ArrowRight, ArrowUp, Sparkles } from "lucide-react";
import { askAssistant, getAssistantStatus } from "../api/assistant";
import { useCurrency } from "../lib/currency";
import { formatLocalTime } from "../lib/datetime";
import { seatClassLabel } from "../lib/seatClass";
import type {
  FlightSearchRequestDto,
  FlightSearchResponseDto,
} from "../types/flight";

/** Matches the backend's @Size(max = 500) so the limit is stated in both places. */
const MAX_LENGTH = 500;

const SUGGESTIONS = [
  "Cheapest flight from Dublin to Paris next Friday",
  "Two business seats to Madrid on the 3rd",
  "Weekend return to Barcelona",
];

/**
 * `flights` is kept per turn rather than hoisted into one "current results"
 * value, so scrolling back shows what was said at the time instead of every
 * earlier answer silently re-pointing at the latest search.
 */
type Turn =
  | { id: number; role: "user"; text: string }
  | {
      id: number;
      role: "assistant";
      text: string;
      flights: FlightSearchResponseDto[];
      interpretedAs: FlightSearchRequestDto | null;
      needsMoreInfo: boolean;
    }
  | { id: number; role: "error"; text: string };

function formatDuration(minutes: number) {
  return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
}

function formatSearchDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
  });
}

/**
 * Deliberately not `FlightResultsList`: that is a full-width card with its own
 * heading and section machinery, and nesting it in a chat bubble reads as a
 * page within a page. Same facts, conversational density.
 */
function ChatFlightRow({
  flight,
  onBook,
}: {
  flight: FlightSearchResponseDto;
  onBook: (flight: FlightSearchResponseDto) => void;
}) {
  const { formatPrice } = useCurrency();

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-zinc-200 bg-white px-3.5 py-3 transition-colors hover:border-zinc-300 dark:border-white/10 dark:bg-white/[0.03] dark:hover:border-white/20">
      <div className="flex items-center gap-3">
        <div className="min-w-[4.5rem]">
          <p className="font-mono text-xs font-semibold text-zinc-900 dark:text-zinc-100">
            {flight.flightNumber}
          </p>
          <p className="text-[11px] text-zinc-400 dark:text-zinc-500">
            {flight.airlineName}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="font-mono text-sm font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {formatLocalTime(flight.departureTime, flight.departureTimezone)}
          </span>
          <span className="flex flex-col items-center text-zinc-300 dark:text-zinc-600">
            <span className="text-[9px]">
              {formatDuration(flight.duration)}
            </span>
            <ArrowRight className="h-2.5 w-6" strokeWidth={1.5} />
          </span>
          <span className="font-mono text-sm font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {formatLocalTime(flight.arrivalTime, flight.arrivalTimezone)}
          </span>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <span className="text-sm font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
          {formatPrice(flight.price)}
        </span>
        <button
          type="button"
          onClick={() => onBook(flight)}
          className="cursor-pointer rounded-lg bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-zinc-800 pointer-coarse:min-h-11 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
        >
          Book
        </button>
      </div>
    </div>
  );
}

/** What the sentence was read as, so a wrong reading is visible not hidden. */
function InterpretationChips({ search }: { search: FlightSearchRequestDto }) {
  const parts = [
    `${search.departureAirport} → ${search.arrivalAirport}`,
    formatSearchDate(search.departureDate),
    search.returnDate ? `back ${formatSearchDate(search.returnDate)}` : null,
    `${search.passengers} passenger${search.passengers > 1 ? "s" : ""}`,
    seatClassLabel(search.seatClass),
  ].filter((p): p is string => p !== null);

  return (
    <div className="flex flex-wrap gap-1.5">
      {parts.map((part) => (
        <span
          key={part}
          className="rounded-full bg-zinc-100 px-2.5 py-0.5 text-[11px] font-medium text-zinc-600 dark:bg-white/10 dark:text-zinc-300"
        >
          {part}
        </span>
      ))}
    </div>
  );
}

interface AssistantChatProps {
  /** Opens the ordinary booking flow, so the assistant is not a second one. */
  onBookFlight: (
    flight: FlightSearchResponseDto,
    search: FlightSearchRequestDto | null,
  ) => void;
  /** Used only when a message names no origin. */
  originHint?: string | null;
}

/**
 * Renders nothing when the backend reports no API key. Offering a chat box that
 * answers 503 on the first message is worse than not offering one, and whether
 * a key exists is a server-side fact the browser cannot infer.
 */
export function AssistantChat({ onBookFlight, originHint }: AssistantChatProps) {
  const [available, setAvailable] = useState<boolean | null>(null);
  const [turns, setTurns] = useState<Turn[]>([]);
  const [draft, setDraft] = useState("");
  const [isSending, setIsSending] = useState(false);
  const nextId = useRef(0);
  const transcriptEnd = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let cancelled = false;
    getAssistantStatus()
      .then((status) => {
        if (!cancelled) setAvailable(status.available);
      })
      // An unreachable status endpoint is treated as "off" rather than surfaced:
      // there is nothing a traveller can do about it, and a broken-looking chat
      // box is worse than no chat box.
      .catch(() => {
        if (!cancelled) setAvailable(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  // Reading the ref inside the effect rather than during render is what keeps
  // this clear of react-hooks/refs.
  useEffect(() => {
    if (turns.length === 0) return;
    transcriptEnd.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [turns]);

  const send = async (message: string) => {
    const text = message.trim();
    if (!text || isSending) return;

    const userTurn: Turn = { id: nextId.current++, role: "user", text };
    setTurns((prev) => [...prev, userTurn]);
    setDraft("");
    setIsSending(true);

    try {
      const response = await askAssistant(text, originHint ?? null);
      setTurns((prev) => [
        ...prev,
        {
          id: nextId.current++,
          role: "assistant",
          text: response.reply,
          flights: response.flights,
          interpretedAs: response.interpretedAs,
          needsMoreInfo: response.needsMoreInfo,
        },
      ]);
    } catch (err) {
      // 503 is the expected failure — the key is missing or the model is
      // unreachable — and it deserves its own wording, because "try again"
      // is useless advice for the first and reasonable for the second.
      const status = axios.isAxiosError(err) ? err.response?.status : undefined;
      setTurns((prev) => [
        ...prev,
        {
          id: nextId.current++,
          role: "error",
          text:
            status === 503
              ? "The assistant is unavailable right now. You can still use the search panel."
              : "Something went wrong. Please try again.",
        },
      ]);
    } finally {
      setIsSending(false);
    }
  };

  // Null while the status is still unknown, so the card does not flash in and
  // then vanish on a deployment without a key.
  if (available !== true) return null;

  const isEmpty = turns.length === 0;

  return (
    <motion.section
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="mt-5 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8"
      aria-labelledby="assistant-heading"
    >
      <div className="mb-4 flex items-center gap-2.5">
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent/10 text-accent">
          <Sparkles className="h-4 w-4" strokeWidth={1.8} />
        </span>
        <div>
          <h2
            id="assistant-heading"
            className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100"
          >
            Ask for a flight
          </h2>
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            Describe your trip and I&apos;ll search the timetable.
          </p>
        </div>
      </div>

      {isEmpty ? (
        <div className="flex flex-wrap gap-2">
          {SUGGESTIONS.map((suggestion) => (
            <button
              key={suggestion}
              type="button"
              onClick={() => void send(suggestion)}
              className="cursor-pointer rounded-full border border-zinc-200 px-3.5 py-1.5 text-xs font-medium text-zinc-600 transition-colors hover:border-zinc-300 hover:text-zinc-900 pointer-coarse:min-h-11 dark:border-white/10 dark:text-zinc-400 dark:hover:border-white/20 dark:hover:text-zinc-100"
            >
              {suggestion}
            </button>
          ))}
        </div>
      ) : (
        <div
          role="log"
          aria-live="polite"
          aria-label="Conversation"
          className="flex max-h-[26rem] flex-col gap-4 overflow-y-auto pr-1"
        >
          {turns.map((turn) =>
            turn.role === "user" ? (
              <p
                key={turn.id}
                className="self-end rounded-2xl rounded-br-sm bg-zinc-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-zinc-900"
              >
                {turn.text}
              </p>
            ) : turn.role === "error" ? (
              <p
                key={turn.id}
                className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400"
              >
                {turn.text}
              </p>
            ) : (
              <div key={turn.id} className="flex flex-col gap-3">
                <p className="max-w-prose text-sm leading-relaxed text-zinc-700 dark:text-zinc-300">
                  {turn.text}
                </p>

                {turn.interpretedAs && (
                  <InterpretationChips search={turn.interpretedAs} />
                )}

                {turn.flights.length > 0 && (
                  <div className="flex flex-col gap-2">
                    {turn.flights.map((flight) => (
                      <ChatFlightRow
                        key={flight.id}
                        flight={flight}
                        onBook={(f) => onBookFlight(f, turn.interpretedAs)}
                      />
                    ))}
                  </div>
                )}
              </div>
            ),
          )}

          {isSending && (
            <p className="flex items-center gap-2.5 text-sm text-zinc-500 dark:text-zinc-400">
              <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
              Searching the timetable…
            </p>
          )}

          <div ref={transcriptEnd} />
        </div>
      )}

      <form
        onSubmit={(e) => {
          e.preventDefault();
          void send(draft);
        }}
        className="mt-5 flex items-center gap-2 rounded-xl border border-zinc-200 bg-white px-4 py-2 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30"
      >
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          maxLength={MAX_LENGTH}
          disabled={isSending}
          // Not "Ask for a flight": that is the region's name, and repeating it
          // here makes a screen reader announce the same phrase twice on entry.
          aria-label="Describe your trip"
          placeholder="Cheapest flight to Paris next Friday"
          className="w-full min-w-0 bg-transparent py-1 text-sm font-medium text-zinc-900 outline-none placeholder:text-zinc-400 disabled:opacity-60 dark:text-zinc-100 dark:placeholder:text-zinc-600"
        />
        <button
          type="submit"
          disabled={isSending || draft.trim().length === 0}
          aria-label="Send"
          className="flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-lg bg-zinc-900 text-white transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-40 pointer-coarse:h-11 pointer-coarse:w-11 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
        >
          <ArrowUp className="h-4 w-4" strokeWidth={2.2} />
        </button>
      </form>
    </motion.section>
  );
}
