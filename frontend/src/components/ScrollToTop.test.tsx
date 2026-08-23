import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Link, MemoryRouter, Route, Routes, useNavigate } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ScrollToTop } from "./ScrollToTop";

/** Back and a same-page query change: the two navigations that must not scroll. */
function Controls() {
  const navigate = useNavigate();
  return (
    <>
      <button type="button" onClick={() => navigate(-1)}>back</button>
      <button type="button" onClick={() => navigate("/explore?country=IE")}>filter</button>
    </>
  );
}

const page = (label: string, extra?: React.ReactNode) => (
  <>
    <p>{label}</p>
    {extra}
    <Controls />
  </>
);

const renderApp = () =>
  render(
    <MemoryRouter initialEntries={["/explore"]}>
      <ScrollToTop />
      <Routes>
        <Route path="/explore" element={page("Explore", <Link to="/explore/DXB">View flights</Link>)} />
        <Route path="/explore/:code" element={page("Destination")} />
      </Routes>
    </MemoryRouter>,
  );

/**
 * The scroll is the easy half; these cover the restraint. Each skip breaks
 * silently — no error, just a page that quietly stops behaving — so a
 * regression would only be found by someone noticing it felt wrong.
 */
describe("ScrollToTop", () => {
  // jsdom has no layout, so the real scrollTo only logs "not implemented".
  let scrollTo: ReturnType<typeof vi.spyOn>;
  beforeEach(() => {
    scrollTo = vi.spyOn(window, "scrollTo").mockImplementation(() => {});
  });

  /** The reported bug: opening a destination from halfway down Explore. */
  it("scrolls to the top when a link opens a new page", async () => {
    renderApp();
    await userEvent.click(screen.getByRole("link", { name: "View flights" }));

    expect(screen.getByText("Destination")).toBeInTheDocument();
    expect(scrollTo).toHaveBeenCalledWith(0, 0);
  });

  /** Back returns to a position the reader chose; the top is not it. */
  it("does not scroll when going back", async () => {
    renderApp();
    await userEvent.click(screen.getByRole("link", { name: "View flights" }));
    scrollTo.mockClear();

    await userEvent.click(screen.getByRole("button", { name: "back" }));

    expect(screen.getByText("Explore")).toBeInTheDocument();
    expect(scrollTo).not.toHaveBeenCalled();
  });

  /** A filter is not a new page, and must not yank the view upward. */
  it("does not scroll when only the query string changes", async () => {
    renderApp();
    await userEvent.click(screen.getByRole("button", { name: "filter" }));

    expect(screen.getByText("Explore")).toBeInTheDocument();
    expect(scrollTo).not.toHaveBeenCalled();
  });
});
