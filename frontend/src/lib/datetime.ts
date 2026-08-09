/**
 * Flight times come back from the API as UTC instants, serialised by Jackson as
 * a bare `LocalDateTime` â€” "2026-08-01T05:30:00", with no zone suffix. JavaScript
 * reads a string in that shape as *local* time, so parsing it directly would
 * silently shift every flight by the viewer's own offset.
 *
 * These helpers append the missing "Z" and then render in whichever airport's
 * zone the caller asks for, so a Tokyo arrival reads in Tokyo time whether the
 * viewer is in Dublin or Los Angeles.
 */

/** Parses an API timestamp as the UTC instant it actually is. */
export function parseApiInstant(iso: string): Date {
  // Already carries a zone (Z or +hh:mm) â€” trust it rather than double-suffixing.
  const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/.test(iso);
  return new Date(hasZone ? iso : `${iso}Z`);
}

/**
 * A missing timezone falls back to the viewer's own, which is what the app did
 * before zones were carried at all. Intl throws on an unknown identifier, so an
 * airport with bad data degrades to local time rather than crashing the page.
 */
function zoneOrLocal(timezone: string | null | undefined): string | undefined {
  return timezone ?? undefined;
}

/**
 * Renders an API timestamp in a specific zone. Exported for the few callers
 * that need a shape the named helpers below don't cover.
 */
export function formatInZone(
  iso: string,
  timezone: string | null | undefined,
  options: Intl.DateTimeFormatOptions,
): string {
  const date = parseApiInstant(iso);
  // Intl throws on an invalid date, and so would the fallback below, which
  // only exists to survive an unusable *zone*. Bail out first so one bad
  // timestamp renders as nothing rather than taking the page down with it.
  if (Number.isNaN(date.getTime())) return "";

  try {
    return new Intl.DateTimeFormat("en-US", {
      ...options,
      timeZone: zoneOrLocal(timezone),
    }).format(date);
  } catch {
    return new Intl.DateTimeFormat("en-US", options).format(date);
  }
}

/** "02:00 PM" at the given airport. */
export function formatLocalTime(iso: string, timezone: string | null | undefined): string {
  return formatInZone(iso, timezone, { hour: "2-digit", minute: "2-digit" });
}

/** "Thu, Jul 30" at the given airport. */
export function formatLocalDate(iso: string, timezone: string | null | undefined): string {
  return formatInZone(iso, timezone, { weekday: "short", day: "numeric", month: "short" });
}

/** "Jul 30, 2026, 02:00 PM" at the given airport. */
export function formatLocalDateTime(iso: string, timezone: string | null | undefined): string {
  return formatInZone(iso, timezone, {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Converts a wall-clock value from a `datetime-local` input, read as time at
 * `timezone`, into the UTC instant the API stores.
 *
 * Works by pretending the string is already UTC, asking what that instant looks
 * like in the target zone, and subtracting the difference. Accurate to the
 * minute except within the hour a zone shifts for daylight saving, where the
 * wall-clock time is genuinely ambiguous anyway.
 */
export function localInputToUtc(
  localValue: string,
  timezone: string | null | undefined,
): string {
  const naive = new Date(`${localValue}:00Z`);
  if (Number.isNaN(naive.getTime())) return localValue;
  if (!timezone) return localValue.length === 16 ? `${localValue}:00` : localValue;

  try {
    const inZone = new Date(naive.toLocaleString("en-US", { timeZone: timezone }));
    const inUtc = new Date(naive.toLocaleString("en-US", { timeZone: "UTC" }));
    const utc = new Date(naive.getTime() - (inZone.getTime() - inUtc.getTime()));
    return utc.toISOString().slice(0, 19);
  } catch {
    return localValue.length === 16 ? `${localValue}:00` : localValue;
  }
}

/** The reverse: a stored UTC instant as a `datetime-local` value in `timezone`. */
export function utcToLocalInput(
  iso: string,
  timezone: string | null | undefined,
): string {
  const date = parseApiInstant(iso);
  if (Number.isNaN(date.getTime())) return "";

  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: timezone ?? undefined,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).formatToParts(date);

  const get = (type: string) => parts.find((p) => p.type === type)?.value ?? "";
  // en-CA renders hour 24 for midnight in some engines; normalise it.
  const hour = get("hour") === "24" ? "00" : get("hour");
  return `${get("year")}-${get("month")}-${get("day")}T${hour}:${get("minute")}`;
}

/** The local calendar date at a given zone, as YYYY-MM-DD. */
function localDateKey(iso: string, timezone: string | null | undefined): string {
  return formatInZone(iso, timezone, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

/**
 * How many local calendar days later the arrival falls — the "+1" an airline
 * timetable puts beside an overnight arrival.
 *
 * Comparing local dates rather than raw elapsed hours is the point: a flight
 * can run thirteen hours and still land the same calendar day heading east,
 * or run two hours and land the next day crossing midnight. Roughly a third of
 * this network's flights land on a later day than they leave, and without this
 * marker "11:30 PM → 05:50 AM" reads as arriving before it departed.
 */
export function arrivalDayOffset(flight: {
  departureTime: string;
  arrivalTime: string;
  departureTimezone: string | null;
  arrivalTimezone: string | null;
}): number {
  const departure = new Date(
    localDateKey(flight.departureTime, flight.departureTimezone),
  );
  const arrival = new Date(
    localDateKey(flight.arrivalTime, flight.arrivalTimezone),
  );
  if (Number.isNaN(departure.getTime()) || Number.isNaN(arrival.getTime())) {
    return 0;
  }
  const msPerDay = 24 * 60 * 60 * 1000;
  return Math.round((arrival.getTime() - departure.getTime()) / msPerDay);
}

/**
 * The airport's UTC offset as "GMT+9", for labelling a time that is not in the
 * viewer's own zone. Long-haul arrival times are otherwise quietly confusing.
 */
export function formatZoneAbbreviation(
  iso: string,
  timezone: string | null | undefined,
): string | null {
  if (!timezone) return null;
  try {
    const parts = new Intl.DateTimeFormat("en-US", {
      timeZone: timezone,
      timeZoneName: "shortOffset",
    }).formatToParts(parseApiInstant(iso));
    return parts.find((p) => p.type === "timeZoneName")?.value ?? null;
  } catch {
    return null;
  }
}
