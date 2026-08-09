import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// Each test starts from a clean document and an empty store. Without this a
// component left mounted by one test is still found by the next one's
// queries, and a token written by one test leaks into the next.
afterEach(() => {
  cleanup();
  localStorage.clear();
});
