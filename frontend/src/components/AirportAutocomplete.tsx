import { useEffect, useRef, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { searchAirports } from "../api/airports";
import type { AirportResponseDto } from "../types/flight";

interface AirportAutocompleteProps {
  label: string;
  value: string;
  onChange: (code: string) => void;
  placeholder: string;
}

export function AirportAutocomplete({
  label,
  value,
  onChange,
  placeholder,
}: AirportAutocompleteProps) {
  const [query, setQuery] = useState(value);
  const [results, setResults] = useState<AirportResponseDto[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(
    undefined,
  );

  // Keep the displayed text in sync when the parent changes the value
  // externally (e.g. the origin/destination swap button). Adjusting during
  // render rather than in an effect means the input never paints the stale
  // text first.
  const [appliedValue, setAppliedValue] = useState(value);
  if (value !== appliedValue) {
    setAppliedValue(value);
    setQuery(value);
  }

  const isSearchable = query.trim().length >= 2;

  useEffect(() => {
    if (!isSearchable) return;

    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      searchAirports(query)
        .then(setResults)
        .catch(() => setResults([]));
    }, 250);

    return () => clearTimeout(debounceRef.current);
  }, [query, isSearchable]);

  // Rather than clearing results when the query gets too short, just stop
  // showing them — that keeps the last fetch usable if the user types again.
  const visibleResults = isSearchable ? results : [];

  const handleSelect = (airport: AirportResponseDto) => {
    setQuery(airport.code);
    onChange(airport.code);
    setResults([]);
    setIsOpen(false);
  };

  return (
    <div className="relative min-w-0 flex-1">
      <label className="group flex flex-col gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
        <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
          {label}
        </span>
        <input
          value={query}
          onChange={(e) => {
            const text = e.target.value.toUpperCase();
            setQuery(text);
            onChange(text.slice(0, 3));
            setIsOpen(true);
          }}
          onFocus={() => setIsOpen(true)}
          onBlur={() => setTimeout(() => setIsOpen(false), 150)}
          maxLength={20}
          placeholder={placeholder}
          className="w-full min-w-0 bg-transparent font-mono text-xl font-semibold tracking-tight text-zinc-900 outline-none placeholder:text-zinc-300 dark:text-zinc-100 dark:placeholder:text-zinc-700"
        />
      </label>

      <AnimatePresence>
        {isOpen && visibleResults.length > 0 && (
          <motion.ul
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            transition={{ duration: 0.15 }}
            className="absolute left-0 right-0 top-full z-20 mt-1.5 max-h-56 overflow-y-auto rounded-xl border border-zinc-200 bg-white p-1 shadow-lg dark:border-white/10 dark:bg-obsidian-raised"
          >
            {visibleResults.map((airport) => (
              <li key={airport.id}>
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => handleSelect(airport)}
                  className="flex w-full items-center justify-between gap-3 rounded-lg px-3 py-2 text-left transition-colors hover:bg-zinc-100 dark:hover:bg-white/5"
                >
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-medium text-zinc-900 dark:text-zinc-100">
                      {airport.city}
                    </span>
                    <span className="block truncate text-xs text-zinc-400 dark:text-zinc-500">
                      {airport.name}
                    </span>
                  </span>
                  <span className="shrink-0 font-mono text-xs font-semibold text-accent">
                    {airport.code}
                  </span>
                </button>
              </li>
            ))}
          </motion.ul>
        )}
      </AnimatePresence>
    </div>
  );
}
