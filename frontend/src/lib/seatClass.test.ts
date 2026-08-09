import { describe, expect, it } from "vitest";
import {
  SEAT_CLASS_LABELS,
  SEAT_CLASS_OPTIONS,
  seatClassLabel,
} from "./seatClass";

describe("seatClassLabel", () => {
  it("gives the display name for each cabin", () => {
    expect(seatClassLabel("ECONOMY")).toBe("Economy");
    expect(seatClassLabel("BUSINESS")).toBe("Business");
    expect(seatClassLabel("FIRST")).toBe("First");
  });

  /**
   * Returning the raw value keeps an unexpected cabin visible on screen
   * rather than silently rendering as blank — the same reasoning as the
   * backend rejecting an unknown seat class instead of defaulting it.
   */
  it("falls back to the raw value for an unknown cabin", () => {
    expect(seatClassLabel("PREMIUM")).toBe("PREMIUM");
    expect(seatClassLabel("")).toBe("");
  });
});

describe("SEAT_CLASS_OPTIONS", () => {
  it("offers exactly the cabins the app knows about", () => {
    expect(SEAT_CLASS_OPTIONS.map((o) => o.value)).toEqual([
      "ECONOMY",
      "BUSINESS",
      "FIRST",
    ]);
  });

  it("uses the same wording as the label lookup, so the two cannot drift", () => {
    for (const option of SEAT_CLASS_OPTIONS) {
      expect(option.label).toBe(SEAT_CLASS_LABELS[option.value]);
      expect(option.label).toBe(seatClassLabel(option.value));
    }
  });
});
