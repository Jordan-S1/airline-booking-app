import { motion } from "framer-motion";
import { Moon, Sun } from "lucide-react";
import { useTheme } from "../lib/theme";

export function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === "dark";

  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label="Toggle color theme"
      aria-pressed={isDark}
      className="relative flex h-8 w-14 items-center cursor-pointer rounded-full border border-zinc-200 bg-zinc-100 px-1 transition-colors dark:border-white/10 dark:bg-white/5"
    >
      <motion.div
        layout
        transition={{ type: "spring", stiffness: 500, damping: 32 }}
        className="flex h-6 w-6 items-center justify-center rounded-full bg-white text-zinc-900 shadow-sm dark:bg-zinc-900 dark:text-zinc-100"
        style={{ marginLeft: isDark ? "calc(100% - 1.5rem)" : "0" }}
      >
        {isDark ? (
          <Moon className="h-3.5 w-3.5" fill="currentColor" />
        ) : (
          <Sun className="h-3.5 w-3.5" />
        )}
      </motion.div>
    </button>
  );
}
