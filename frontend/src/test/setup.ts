import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

// jsdom implements no layout, so it has no scrollIntoView. Components that
// keep something in view call it during a normal render and would throw here
// for a reason that has nothing to do with what is being tested. Stubbed
// globally rather than guarded at each call site, so the production code is
// not shaped around the test environment.
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = vi.fn();
}

// Each test starts from a clean document and an empty store. Without this a
// component left mounted by one test is still found by the next one's
// queries, and a token written by one test leaks into the next.
afterEach(() => {
  cleanup();
  localStorage.clear();
});
