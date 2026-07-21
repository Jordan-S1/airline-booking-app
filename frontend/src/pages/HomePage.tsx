import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight, Globe2, ShieldCheck, Sparkles } from "lucide-react";

const FEATURES = [
  {
    icon: Globe2,
    title: "Global network",
    body: "Search live availability across every SkyAir route from a single, elegant interface.",
  },
  {
    icon: Sparkles,
    title: "Effortless booking",
    body: "From search to confirmation in a few taps — passenger details, payment, done.",
  },
  {
    icon: ShieldCheck,
    title: "Secure by design",
    body: "Token-based authentication and encrypted checkout keep every trip protected.",
  },
];

const ROUTES = ["DUB → LHR", "LHR → MAD", "DUB → CDG", "FRA → AMS", "DUB → BCN"];

export function HomePage() {
  return (
    <main className="mx-auto max-w-6xl">
      <section className="flex flex-col items-center pt-16 pb-20 text-center sm:pt-24">
        <motion.span
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mt-16 inline-flex items-center gap-2 rounded-full border border-zinc-200 bg-white/60 px-3 py-1 text-xs font-medium text-zinc-500 backdrop-blur-md dark:border-white/10 dark:bg-white/5 dark:text-zinc-400 sm:mt-0"
        >
          <span className="h-1.5 w-1.5 rounded-full bg-accent" />
          The premium way to fly
        </motion.span>

        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.05 }}
          className="mt-6 max-w-3xl text-4xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100 sm:text-6xl"
        >
          Travel, reimagined for the modern flyer.
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.12 }}
          className="mt-5 max-w-xl text-base text-zinc-500 dark:text-zinc-400"
        >
          SkyAir brings search, booking, live flight status and trip management
          into one beautifully considered experience.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.18 }}
          className="mt-8 flex flex-col items-center gap-3 sm:flex-row"
        >
          <Link
            to="/dashboard"
            className="flex items-center gap-2 rounded-xl bg-zinc-900 px-5 py-3 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
          >
            Book a flight
            <ArrowRight className="h-4 w-4" strokeWidth={2} />
          </Link>
          <Link
            to="/explore"
            className="rounded-xl border border-zinc-200 px-5 py-3 text-sm font-semibold text-zinc-700 transition-colors hover:bg-zinc-100 dark:border-white/10 dark:text-zinc-200 dark:hover:bg-white/5"
          >
            Explore destinations
          </Link>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.28 }}
          className="mt-10 flex flex-wrap items-center justify-center gap-2"
        >
          {ROUTES.map((route) => (
            <span
              key={route}
              className="rounded-lg border border-zinc-200 px-3 py-1.5 font-mono text-xs font-medium text-zinc-500 dark:border-white/10 dark:text-zinc-400"
            >
              {route}
            </span>
          ))}
        </motion.div>
      </section>

      <section className="grid grid-cols-1 gap-5 pb-8 md:grid-cols-3">
        {FEATURES.map((feature, i) => (
          <motion.div
            key={feature.title}
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5, delay: i * 0.08 }}
            className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none"
          >
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-zinc-100 text-zinc-900 dark:bg-white/5 dark:text-accent">
              <feature.icon className="h-5 w-5" strokeWidth={1.8} />
            </span>
            <h3 className="mt-4 text-base font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
              {feature.title}
            </h3>
            <p className="mt-1.5 text-sm text-zinc-500 dark:text-zinc-400">
              {feature.body}
            </p>
          </motion.div>
        ))}
      </section>
    </main>
  );
}
