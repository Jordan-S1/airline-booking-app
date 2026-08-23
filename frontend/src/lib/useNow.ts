import { useEffect, useState } from "react";

/**
 * A clock that re-renders on an interval.
 *
 * Anything showing "the time right now" is wrong the moment it renders and
 * gets worse the longer a tab sits open — a destination page left open over
 * lunch would claim the local time it had an hour ago. Thirty seconds is fine
 * for minute-resolution display: the reading is never more than half a minute
 * stale, and the re-render is a formatted string.
 *
 * @returns epoch milliseconds, changing on each tick
 */
export function useNow(intervalMs = 30_000): number {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);

  return now;
}
