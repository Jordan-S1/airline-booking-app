import { describe, expect, it } from "vitest";
import {
  arrivalDayOffset,
  formatLocalDate,
  formatLocalTime,
  localInputToUtc,
  parseApiInstant,
  utcToLocalInput,
} from "./datetime";

/**
 * The API serialises flight times as bare LocalDateTime strings that are
 * really UTC instants. Everything here guards the one mistake that shifts
 * every flight on the page by the viewer's own offset.
 */
describe("parseApiInstant", () => {
  it("reads a zone-less API timestamp as UTC, not as the viewer's local time", () => {
    expect(parseApiInstant("2026-08-01T05:30:00").toISOString()).toBe(
      "2026-08-01T05:30:00.000Z",
    );
  });

  it("leaves a timestamp that already carries a zone alone", () => {
    expect(parseApiInstant("2026-08-01T05:30:00Z").toISOString()).toBe(
      "2026-08-01T05:30:00.000Z",
    );
    expect(parseApiInstant("2026-08-01T07:30:00+02:00").toISOString()).toBe(
      "2026-08-01T05:30:00.000Z",
    );
  });
});

describe("formatLocalTime", () => {
  it("renders one instant differently in each airport's own zone", () => {
    const departure = "2026-08-01T05:30:00"; // 06:30 in Dublin, 07:30 in Madrid
    expect(formatLocalTime(departure, "Europe/Dublin")).toBe("06:30 AM");
    expect(formatLocalTime(departure, "Europe/Madrid")).toBe("07:30 AM");
    expect(formatLocalTime(departure, "UTC")).toBe("05:30 AM");
  });

  it("handles a zone far from the viewer's without shifting the date", () => {
    // 23:30 UTC is already the next morning in Tokyo.
    expect(formatLocalTime("2026-08-01T23:30:00", "Asia/Tokyo")).toBe("08:30 AM");
    expect(formatLocalDate("2026-08-01T23:30:00", "Asia/Tokyo")).toContain("Aug 2");
  });

  it("falls back to the viewer's zone rather than throwing on a bad identifier", () => {
    expect(() => formatLocalTime("2026-08-01T05:30:00", "Not/AZone")).not.toThrow();
  });
});

describe("arrivalDayOffset", () => {
  const flight = (
    departureTime: string,
    arrivalTime: string,
    departureTimezone: string | null = "UTC",
    arrivalTimezone: string | null = "UTC",
  ) => ({ departureTime, arrivalTime, departureTimezone, arrivalTimezone });

  it("is 0 for a flight that lands the same day", () => {
    expect(
      arrivalDayOffset(flight("2026-08-01T05:30:00", "2026-08-01T06:55:00")),
    ).toBe(0);
  });

  it("is 1 for a flight that lands after midnight", () => {
    expect(
      arrivalDayOffset(flight("2026-08-01T22:30:00", "2026-08-02T01:15:00")),
    ).toBe(1);
  });

  it("counts days in each airport's own zone, not in UTC", () => {
    // Departs 23:30 UTC on the 1st, lands 01:30 UTC on the 2nd — but in Tokyo
    // both are on the 2nd, so for the traveller it is not a next-day arrival.
    expect(
      arrivalDayOffset(
        flight(
          "2026-08-01T23:30:00",
          "2026-08-02T01:30:00",
          "Asia/Tokyo",
          "Asia/Tokyo",
        ),
      ),
    ).toBe(0);
  });

  it("can be negative when crossing the date line eastbound", () => {
    // Leaves Tokyo on the 2nd local, lands in Los Angeles on the 1st local.
    expect(
      arrivalDayOffset(
        flight(
          "2026-08-01T16:00:00",
          "2026-08-02T01:00:00",
          "Asia/Tokyo",
          "America/Los_Angeles",
        ),
      ),
    ).toBeLessThan(0);
  });

  it("returns 0 rather than NaN for an unparseable timestamp", () => {
    expect(arrivalDayOffset(flight("nonsense", "2026-08-01T06:55:00"))).toBe(0);
  });
});

describe("localInputToUtc / utcToLocalInput", () => {
  it("round-trips a wall-clock time through the airport's zone", () => {
    const typed = "2026-08-01T06:30";
    const stored = localInputToUtc(typed, "Europe/Dublin");

    expect(stored).toBe("2026-08-01T05:30:00"); // Dublin is UTC+1 in August
    expect(utcToLocalInput(stored, "Europe/Dublin")).toBe(typed);
  });

  it("round-trips across a zone well ahead of UTC", () => {
    const typed = "2026-08-02T08:30";
    const stored = localInputToUtc(typed, "Asia/Tokyo");

    expect(utcToLocalInput(stored, "Asia/Tokyo")).toBe(typed);
  });

  it("treats the value as already-UTC when no zone is known", () => {
    expect(localInputToUtc("2026-08-01T06:30", null)).toBe("2026-08-01T06:30:00");
  });

  it("returns an empty string for an unparseable stored value", () => {
    expect(utcToLocalInput("nonsense", "Europe/Dublin")).toBe("");
  });
});
