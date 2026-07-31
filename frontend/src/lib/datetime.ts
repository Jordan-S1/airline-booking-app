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
