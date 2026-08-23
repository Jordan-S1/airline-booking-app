import { useEffect, useRef } from "react";
import { useLocation, useNavigationType } from "react-router-dom";

/**
 * Sends each newly opened page to the top.
 *
 * A browser resets scroll on a full page load. A client-side navigation only
 * swaps the component tree, so the document keeps the offset it already had —
 * which is why opening a destination from halfway down Explore landed you
 * halfway down the destination.
 *
 * Two things it deliberately does not do:
 *
 * Back and forward are left alone. That scroll position is one the reader
 * chose, and `history.scrollRestoration` returns them to it; overriding it
 * would dump them at the top of a page they had already read.
 *
 * A query-string change is not a new page. Filters and `?token=` links keep the
 * same pathname, and yanking the view to the top mid-interaction is worse than
 * doing nothing. Hence the pathname guard rather than reacting to any location
 * change.
 *
 * React Router ships `<ScrollRestoration />` for this, but only for data
 * routers created with `createBrowserRouter`. This app uses `<BrowserRouter>`
 * with the component `<Routes>` API, where it is unavailable.
 */
export function ScrollToTop() {
  const { pathname } = useLocation();
  const navigationType = useNavigationType();
  const lastPathname = useRef<string | null>(null);

  useEffect(() => {
    if (lastPathname.current === pathname) return;
    lastPathname.current = pathname;

    // POP covers back, forward, and the first render of a fresh load — all
    // cases where the browser already owns the scroll position.
    if (navigationType === "POP") return;

    window.scrollTo(0, 0);
  }, [pathname, navigationType]);

  return null;
}
