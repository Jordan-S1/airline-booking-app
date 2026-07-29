import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { Check } from "lucide-react";
import { useCurrency } from "../lib/currency";
import { getBooking } from "../api/bookings";
import type { BookingResponseDto } from "../types/booking";

type Status = "loading" | "ready" | "error";

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString("en-US", {
    weekday: "short",
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between py-3">
      <span className="text-sm text-zinc-500 dark:text-zinc-400">{label}</span>
      <span className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
        {value}
      </span>
    </div>
  );
}

export function BookingConfirmationPage() {
  const { reference } = useParams<{ reference: string }>();
  const { formatPrice } = useCurrency();
  const [status, setStatus] = useState<Status>("loading");
  const [booking, setBooking] = useState<BookingResponseDto | null>(null);

  useEffect(() => {
    if (!reference) return;
    getBooking(reference)
      .then((data) => {
        setBooking(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, [reference]);

  return (
    <main className="mx-auto mt-10 max-w-2xl">
      {status === "loading" && (
        <div className="flex items-center gap-3 pt-8 text-sm text-zinc-500 dark:text-zinc-400">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
          Loading your booking…
        </div>
      )}

      {status === "error" && (
        <p className="mt-8 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
          We couldn't find that booking.
        </p>
      )}

      {status === "ready" && booking && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="overflow-hidden rounded-2xl border border-zinc-200 bg-white shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none"
        >
          <div className="relative flex flex-col items-center border-b border-zinc-100 px-6 py-10 text-center dark:border-white/5">
            {/* inset-0 rather than a fixed h-32: the glow has to reach the
                divider below, and the header's height varies with content.
                Sizing the ellipse to the box (100% 100%) makes the fade finish
                exactly at the border instead of stopping short of it. */}
            <div
              aria-hidden
              className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_100%_100%_at_50%_0%,rgba(6,182,212,0.16),transparent_100%)]"
            />
            <span className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-400/10 dark:text-emerald-400">
              <Check className="h-6 w-6" strokeWidth={2.2} />
            </span>
            <h1 className="mt-4 text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              Booking confirmed
            </h1>
            <p className="mt-1 font-mono text-sm text-zinc-500 dark:text-zinc-400">
              {booking.bookingReference}
            </p>
          </div>

          <div className="px-6 py-6 sm:px-8">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <p className="font-mono text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
                  {booking.departureAirport}
                </p>
                <p className="mt-1 text-xs text-zinc-400 dark:text-zinc-500">
                  {formatDateTime(booking.departureTime)}
                </p>
              </div>
              <span className="font-mono text-sm text-zinc-400 dark:text-zinc-500">
                {booking.flightNumber}
              </span>
              <div className="text-right">
                <p className="font-mono text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
                  {booking.arrivalAirport}
                </p>
                <p className="mt-1 text-xs text-zinc-400 dark:text-zinc-500">
                  {formatDateTime(booking.arrivalTime)}
                </p>
              </div>
            </div>

            <div className="divide-y divide-zinc-100 border-t border-zinc-100 dark:divide-white/5 dark:border-white/5">
              <DetailRow label="Status" value={booking.status} />
              <DetailRow
                label="Passengers"
                value={String(booking.numberOfPassengers)}
              />
              <DetailRow
                label="Cabin"
                value={
                  booking.seatClass.charAt(0) +
                  booking.seatClass.slice(1).toLowerCase()
                }
              />
              <DetailRow
                label="Total paid"
                value={formatPrice(booking.totalAmount)}
              />
            </div>

            <div className="mt-6 flex gap-3">
              <Link
                to="/trips"
                className="flex-1 rounded-xl bg-zinc-900 py-3 text-center text-sm font-semibold text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
              >
                View all trips
              </Link>
              <Link
                to="/dashboard"
                className="flex-1 rounded-xl border border-zinc-200 py-3 text-center text-sm font-semibold text-zinc-700 transition-colors hover:bg-zinc-100 dark:border-white/10 dark:text-zinc-200 dark:hover:bg-white/5"
              >
                Back to dashboard
              </Link>
            </div>
          </div>
        </motion.div>
      )}
    </main>
  );
}
