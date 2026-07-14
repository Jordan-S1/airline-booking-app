import { useState } from "react";
import { motion } from "framer-motion";
import { ThemeToggle } from "./ThemeToggle";
import { AuthModal } from "./AuthModal";
import { useAuth } from "../lib/auth";

const NAV_LINKS = ["Dashboard", "Trips", "Explore", "Loyalty"];

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  return (
    <>
      <motion.header
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
        className="sticky top-4 z-50 mx-auto flex w-full max-w-6xl items-center justify-between rounded-2xl border border-zinc-200/80 bg-white/70 px-4 py-3 shadow-sm backdrop-blur-md dark:border-white/10 dark:bg-white/5 dark:shadow-none"
      >
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-2">
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-zinc-900 text-sm font-bold text-white dark:bg-white dark:text-zinc-900">
              S
            </span>
            <span className="text-sm font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
              SkyAir
            </span>
          </div>

          <nav className="hidden items-center gap-6 md:flex">
            {NAV_LINKS.map((link, i) => (
              <a
                key={link}
                href="#"
                className={`text-sm transition-colors ${
                  i === 0
                    ? "font-medium text-zinc-900 dark:text-zinc-100"
                    : "text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
                }`}
              >
                {link}
              </a>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          <ThemeToggle />
          {isAuthenticated && user ? (
            <div className="flex items-center gap-3">
              <span className="hidden text-sm font-medium text-zinc-600 dark:text-zinc-300 sm:inline">
                {user.firstName}
              </span>
              <button
                type="button"
                onClick={logout}
                className="rounded-xl cursor-pointer hover:text-red-400 border border-zinc-200 px-4 py-2 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 dark:border-white/10 dark:text-zinc-300 dark:hover:bg-white/5"
              >
                Sign out
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setIsAuthModalOpen(true)}
              className="rounded-xl cursor-pointer bg-zinc-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
            >
              Sign in
            </button>
          )}
        </div>
      </motion.header>

      {isAuthModalOpen && (
        <AuthModal onClose={() => setIsAuthModalOpen(false)} />
      )}
    </>
  );
}
