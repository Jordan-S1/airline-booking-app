import { useEffect, useState } from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { Menu, X } from "lucide-react";
import { ThemeToggle } from "./ThemeToggle";
import { AuthModal } from "./AuthModal";
import { isAdmin, useAuth } from "../lib/auth";

const NAV_LINKS: {
  label: string;
  to: string;
  protected?: boolean;
  adminOnly?: boolean;
}[] = [
  { label: "Dashboard", to: "/dashboard" },
  { label: "Explore", to: "/explore" },
  { label: "Trips", to: "/trips", protected: true },
  { label: "Admin", to: "/admin", adminOnly: true },
];

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const location = useLocation();

  // The drawer is keyed by the route it was opened on rather than held as a
  // plain boolean, so navigating away closes it as a consequence of the route
  // changing instead of needing an effect to synchronise the two.
  const [menuPath, setMenuPath] = useState<string | null>(null);
  const isMenuOpen = menuPath === location.pathname;
  const closeMenu = () => setMenuPath(null);

  const userIsAdmin = isAdmin(user);
  const visibleLinks = NAV_LINKS.filter((link) => {
    if (link.adminOnly) return userIsAdmin;
    return !link.protected || isAuthenticated;
  });

  // While the drawer covers the page, the page behind it should not scroll.
  useEffect(() => {
    if (!isMenuOpen) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setMenuPath(null);
    };
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isMenuOpen]);

  return (
    <>
      <AnimatePresence>
        {isMenuOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            onClick={closeMenu}
            aria-hidden
            className="fixed inset-0 z-40 bg-zinc-900/20 backdrop-blur-sm md:hidden dark:bg-black/40"
          />
        )}
      </AnimatePresence>

      <motion.header
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
        className="sticky top-4 z-50 mx-auto flex w-full max-w-6xl items-center justify-between rounded-2xl border border-zinc-200/80 bg-white/70 px-4 py-3 shadow-sm backdrop-blur-md dark:border-white/10 dark:bg-white/5 dark:shadow-none"
      >
        <div className="flex items-center gap-8">
          <Link
            to="/"
            className="flex items-center gap-2 pointer-coarse:min-h-11"
          >
            {/* No badge: the mark is the aircraft itself. It keeps the 28px
                footprint of the old badge so the lockup and the header height
                are unchanged. */}
            <img
              src="/airplane.png"
              alt=""
              className="h-7 w-7 shrink-0 object-contain"
            />
            <span className="text-sm font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
              SkyAir
            </span>
          </Link>

          <nav className="hidden items-center gap-6 md:flex">
            {visibleLinks.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  `text-sm transition-colors ${
                    isActive
                      ? "font-medium text-zinc-900 dark:text-zinc-100"
                      : "text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          {/* On a phone the toggle moves into the drawer, so the header has room
              for the sign-in call to action rather than burying it. */}
          <span className="hidden md:inline">
            <ThemeToggle />
          </span>

          {isAuthenticated && user ? (
            <div className="flex items-center gap-3">
              <Link
                to="/profile"
                className="hidden text-sm font-medium text-zinc-600 transition-colors hover:text-zinc-900 md:inline dark:text-zinc-300 dark:hover:text-zinc-100"
              >
                {user.firstName}
              </Link>
              <button
                type="button"
                onClick={logout}
                className="inline-flex cursor-pointer items-center rounded-xl border border-zinc-200 px-4 py-2 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 hover:text-red-400 pointer-coarse:min-h-11 dark:border-white/10 dark:text-zinc-300 dark:hover:bg-white/5"
              >
                Sign out
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setIsAuthModalOpen(true)}
              className="inline-flex cursor-pointer items-center rounded-xl bg-zinc-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-800 pointer-coarse:min-h-11 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
            >
              Sign in
            </button>
          )}

          <button
            type="button"
            onClick={() => (isMenuOpen ? closeMenu() : setMenuPath(location.pathname))}
            aria-label={isMenuOpen ? "Close menu" : "Open menu"}
            aria-expanded={isMenuOpen}
            aria-controls="mobile-nav"
            className="flex h-11 w-11 cursor-pointer items-center justify-center rounded-xl border border-zinc-200 text-zinc-600 transition-colors hover:bg-zinc-100 md:hidden dark:border-white/10 dark:text-zinc-300 dark:hover:bg-white/5"
          >
            {isMenuOpen ? (
              <X className="h-5 w-5" strokeWidth={1.8} />
            ) : (
              <Menu className="h-5 w-5" strokeWidth={1.8} />
            )}
          </button>
        </div>

        <AnimatePresence>
          {isMenuOpen && (
            <motion.nav
              id="mobile-nav"
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
              className="absolute left-0 right-0 top-full mt-2 rounded-2xl border border-zinc-200/80 bg-white p-2 shadow-lg md:hidden dark:border-white/10 dark:bg-obsidian-raised"
            >
              {visibleLinks.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  onClick={closeMenu}
                  className={({ isActive }) =>
                    `flex min-h-11 items-center rounded-xl px-4 text-sm transition-colors ${
                      isActive
                        ? "bg-zinc-100 font-medium text-zinc-900 dark:bg-white/10 dark:text-zinc-100"
                        : "text-zinc-600 hover:bg-zinc-50 dark:text-zinc-300 dark:hover:bg-white/5"
                    }`
                  }
                >
                  {link.label}
                </NavLink>
              ))}

              {isAuthenticated && user && (
                <NavLink
                  to="/profile"
                  onClick={closeMenu}
                  className={({ isActive }) =>
                    `flex min-h-11 items-center rounded-xl px-4 text-sm transition-colors ${
                      isActive
                        ? "bg-zinc-100 font-medium text-zinc-900 dark:bg-white/10 dark:text-zinc-100"
                        : "text-zinc-600 hover:bg-zinc-50 dark:text-zinc-300 dark:hover:bg-white/5"
                    }`
                  }
                >
                  Profile
                </NavLink>
              )}

              <div className="mt-1 flex min-h-11 items-center justify-between gap-3 border-t border-zinc-200/80 px-4 pt-2 dark:border-white/10">
                <span className="text-sm text-zinc-500 dark:text-zinc-400">
                  Appearance
                </span>
                <ThemeToggle />
              </div>
            </motion.nav>
          )}
        </AnimatePresence>
      </motion.header>

      {isAuthModalOpen && (
        <AuthModal onClose={() => setIsAuthModalOpen(false)} />
      )}
    </>
  );
}
