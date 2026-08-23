import { Outlet } from "react-router-dom";
import { Navbar } from "./Navbar";
import { Footer } from "./Footer";
import { ScrollToTop } from "./ScrollToTop";

export function Layout() {
  return (
    <div className="relative min-h-screen overflow-x-hidden px-4 pb-20 sm:px-6 lg:px-8">
      {/* Renders nothing; resets scroll when the route changes. */}
      <ScrollToTop />

      {/* Ambient background glow */}
      <div
        aria-hidden
        className="pointer-events-none fixed inset-0 -z-10 bg-[radial-gradient(circle_at_20%_-10%,rgba(6,182,212,0.10),transparent_45%),radial-gradient(circle_at_85%_10%,rgba(6,182,212,0.06),transparent_40%)] dark:bg-[radial-gradient(circle_at_20%_-10%,rgba(6,182,212,0.14),transparent_45%),radial-gradient(circle_at_85%_10%,rgba(6,182,212,0.08),transparent_40%)]"
      />
      <Navbar />
      <Outlet />
      <Footer />
    </div>
  );
}
