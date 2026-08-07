import type { SeatClass } from "../types/flight";

/**
 * Display names for the cabins.
 *
 * <p>Lives here rather than beside the search panel's select so the label
 * shown against a set of results is necessarily the same wording as the
 * control that chose it, and so importing it does not pull a component
 * module into places that only need the text.
 */
export const SEAT_CLASS_LABELS: Record<SeatClass, string> = {
  ECONOMY: "Economy",
  BUSINESS: "Business",
  FIRST: "First",
};

export const SEAT_CLASS_OPTIONS: { value: SeatClass; label: string }[] = (
  Object.keys(SEAT_CLASS_LABELS) as SeatClass[]
).map((value) => ({ value, label: SEAT_CLASS_LABELS[value] }));

/** Falls back to the raw value so an unexpected cabin is visible, not hidden. */
export function seatClassLabel(value: string): string {
  return SEAT_CLASS_LABELS[value as SeatClass] ?? value;
}
