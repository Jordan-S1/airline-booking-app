import { Link } from "react-router-dom";

const REPO_URL = "https://github.com/Jordan-S1/airline-booking-app";

/** Small muted link, sized for touch on coarse pointers. */
const LINK =
  "inline-flex items-center text-zinc-500 transition-colors hover:text-zinc-900 pointer-coarse:min-h-11 dark:text-zinc-400 dark:hover:text-zinc-100";

/**
 * Site footer.
 * It carries the Flaticon attribution, which the free licence requires wherever the icon is used.
 */
export function Footer() {
  return (
    <footer className="mx-auto mt-24 w-full max-w-6xl border-t border-zinc-200/80 pt-8 dark:border-white/10">
      <div className="flex flex-col gap-8 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <img
              src="/airplane.png"
              alt=""
              className="h-5 w-5 shrink-0 object-contain"
            />
            <span className="text-sm font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
              SkyAir
            </span>
          </div>
          <p className="mt-2 max-w-xs text-xs leading-relaxed text-zinc-500 dark:text-zinc-400">
            An airline app. Flight search, booking and live aircraft tracking.
          </p>
        </div>

        <nav className="flex flex-wrap items-center gap-x-6 gap-y-1 text-xs">
          <Link to="/explore" className={LINK}>
            Explore
          </Link>
          <Link to="/dashboard" className={LINK}>
            Dashboard
          </Link>
          <a
            href={REPO_URL}
            target="_blank"
            rel="noopener noreferrer"
            className={LINK}
          >
            GitHub
          </a>
        </nav>
      </div>

      <div className="mt-8 flex flex-col gap-1 border-t border-zinc-200/60 pt-5 text-[11px] text-zinc-400 dark:border-white/5 dark:text-zinc-500 sm:flex-row sm:items-center sm:justify-between">
        <p>© {new Date().getFullYear()} Jordan Shodipo.</p>
        {/* Wording, link and title are fixed by Flaticon's attribution terms. */}
        <p>
          <a
            href="https://www.flaticon.com/free-icons/plane"
            title="plane icons"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center underline decoration-dotted underline-offset-2 transition-colors hover:text-zinc-600 pointer-coarse:min-h-11 dark:hover:text-zinc-300"
          >
            Plane icons created by Konkapp - Flaticon
          </a>
        </p>
      </div>
    </footer>
  );
}
