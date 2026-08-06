import * as Select from "@radix-ui/react-select";
import { Check, ChevronDown } from "lucide-react";

export interface SelectOption<T extends string | number> {
  value: T;
  label: string;
  /** Optional secondary text shown to the right of the label in the list. */
  hint?: string;
}

interface SelectFieldProps<T extends string | number> {
  label: string;
  value: T;
  options: SelectOption<T>[];
  onChange: (value: T) => void;
}

/**
 * A styled replacement for `<select>`.
 *
 * A native select's option list is rendered by the OS, so it can't be themed —
 * it always shows the system font and highlight colour. This renders the list
 * as real DOM so it matches the app in both light and dark mode.
 *
 * <p>Built on Radix Select rather than hand-rolled. The keyboard and ARIA
 * behaviour a listbox needs is deceptively large — roving focus with
 * `aria-activedescendant` so a screen reader announces the highlighted option
 * and not merely the selected one, typeahead, collision-aware positioning,
 * scroll locking and focus return. The hand-written version covered the
 * obvious half of that.
 *
 * <p>Radix ships no styles, so every class below is the same one the previous
 * implementation used and the control looks unchanged.
 */
export function SelectField<T extends string | number>({
  label,
  value,
  options,
  onChange,
}: SelectFieldProps<T>) {
  // Radix works in strings. The option list is the source of truth for turning
  // one back into the caller's own type, which keeps numeric options working.
  const handleChange = (next: string) => {
    const match = options.find((option) => String(option.value) === next);
    if (match) onChange(match.value);
  };

  return (
    <Select.Root value={String(value)} onValueChange={handleChange}>
      {/* The trigger is the whole bordered card, so the entire control is the
          hit area natively — the previous version needed a stretched ::after
          to widen a 20px target. */}
      <Select.Trigger
        aria-label={label}
        className="relative flex w-full min-w-0 cursor-pointer flex-col gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-left outline-none transition-colors focus:border-zinc-400 data-[state=open]:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus:border-white/30 dark:data-[state=open]:border-white/30"
      >
        <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
          {label}
        </span>
        <span className="flex w-full min-w-0 items-center justify-between gap-2 text-sm font-medium text-zinc-900 dark:text-zinc-100">
          <Select.Value className="truncate" />
          <Select.Icon asChild>
            <ChevronDown
              strokeWidth={1.8}
              className="h-3.5 w-3.5 shrink-0 text-zinc-400 transition-transform dark:text-zinc-500"
            />
          </Select.Icon>
        </span>
      </Select.Trigger>

      <Select.Portal>
        <Select.Content
          position="popper"
          sideOffset={6}
          // Matching the trigger width keeps the list aligned to the card, as
          // the absolutely positioned list used to be.
          className="pop z-50 max-h-56 w-[var(--radix-select-trigger-width)] overflow-hidden rounded-xl border border-zinc-200 bg-white shadow-lg dark:border-white/10 dark:bg-obsidian-raised"
        >
          <Select.Viewport className="p-1">
            {options.map((option) => (
              <Select.Item
                key={String(option.value)}
                value={String(option.value)}
                className="group flex w-full cursor-pointer select-none items-center justify-between gap-2 rounded-lg px-3 py-2 text-left text-sm text-zinc-600 outline-none transition-colors data-[highlighted]:bg-zinc-100 data-[highlighted]:text-zinc-900 data-[state=checked]:bg-accent/10 data-[state=checked]:font-medium data-[state=checked]:text-accent dark:text-zinc-300 dark:data-[highlighted]:bg-white/5 dark:data-[highlighted]:text-zinc-100"
              >
                <Select.ItemText>{option.label}</Select.ItemText>
                {option.hint && (
                  <span className="shrink-0 text-xs text-zinc-400 group-data-[state=checked]:hidden dark:text-zinc-500">
                    {option.hint}
                  </span>
                )}
                <Select.ItemIndicator>
                  <Check className="h-3.5 w-3.5 shrink-0" strokeWidth={2.5} />
                </Select.ItemIndicator>
              </Select.Item>
            ))}
          </Select.Viewport>
        </Select.Content>
      </Select.Portal>
    </Select.Root>
  );
}
