import { useEffect, useRef, useState } from "react";
import { useCombobox } from "downshift";
import { searchAirports } from "../api/airports";
import type { AirportResponseDto } from "../types/flight";

interface AirportAutocompleteProps {
  label: string;
  value: string;
  onChange: (code: string) => void;
  placeholder: string;
}

/**
 * Airport picker with async suggestions.
 *
 * <p>Built on Downshift's `useCombobox` rather than hand-rolled.
 * the combobox pattern is arrow-key traversal,
 * Enter to commit, Escape to dismiss, and `aria-activedescendant`
 * so a screen reader announces the option being traversed.
 *
 */
export function AirportAutocomplete({
  label,
  value,
  onChange,
  placeholder,
}: AirportAutocompleteProps) {
  const [query, setQuery] = useState(value);
  const [results, setResults] = useState<AirportResponseDto[]>([]);

  const lastEmitted = useRef(value);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(
    undefined,
  );

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

  const {
    isOpen,
    getLabelProps,
    getInputProps,
    getMenuProps,
    getItemProps,
    highlightedIndex,
    setInputValue,
  } = useCombobox({
    items: visibleResults,
    // The input text is left entirely to Downshift. Controlling `inputValue`
    // here as well gave the field two owners, and because the text was
    // upper-cased on the way through
    itemToString: (item) => item?.code ?? "",
    onInputValueChange: ({ inputValue }) => {
      const text = (inputValue ?? "").toUpperCase();
      setQuery(text);
      const code = text.slice(0, 3);
      lastEmitted.current = code;
      onChange(code);
    },
    // Deliberately no onSelectedItemChange. Downshift already sets its input
    // value to itemToString(item) when an item is chosen, which lands in
    // onInputValueChange above.
  });

  // The swap button rewrites `value` from outside, and Downshift owns the
  // text, so that change has to be pushed in. The guard keeps the parent's
  // echo of what is currently being typed from rewriting the field mid-word.
  useEffect(() => {
    if (value !== lastEmitted.current) {
      lastEmitted.current = value;
      setInputValue(value);
    }
  }, [value, setInputValue]);

  return (
    <div className="relative min-w-0 flex-1">
      <label
        {...getLabelProps()}
        className="group flex flex-col gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30"
      >
        <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
          {label}
        </span>
        <input
          {...getInputProps({
            maxLength: 20,
            placeholder,
            className:
              "w-full min-w-0 bg-transparent font-mono text-xl font-semibold uppercase tracking-tight text-zinc-900 outline-none placeholder:text-zinc-300 dark:text-zinc-100 dark:placeholder:text-zinc-700",
          })}
        />
      </label>

      {/* Downshift needs its ref on the list at all times, so this stays
          mounted and is hidden rather than unmounted when closed. */}
      <ul
        {...getMenuProps()}
        data-state={isOpen && visibleResults.length > 0 ? "open" : "closed"}
        className={`pop absolute left-0 right-0 top-full z-20 mt-1.5 max-h-56 overflow-y-auto rounded-xl border border-zinc-200 bg-white p-1 shadow-lg dark:border-white/10 dark:bg-obsidian-raised ${
          isOpen && visibleResults.length > 0 ? "" : "hidden"
        }`}
      >
        {isOpen &&
          visibleResults.map((airport, index) => (
            <li
              key={airport.id}
              {...getItemProps({ item: airport, index })}
              className={`flex w-full cursor-pointer items-center justify-between gap-3 rounded-lg px-3 py-2 text-left transition-colors ${
                highlightedIndex === index ? "bg-zinc-100 dark:bg-white/5" : ""
              }`}
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
            </li>
          ))}
      </ul>
    </div>
  );
}
