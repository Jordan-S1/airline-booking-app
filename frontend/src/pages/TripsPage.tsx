import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight, Plane } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { useAuth } from "../lib/auth";
import { useCurrency } from "../lib/currency";
import { formatInZone } from "../lib/datetime";
import { getBookingsByUser, cancelBooking } from "../api/bookings";
import type { BookingResponseDto } from "../types/booking";

type Status = "loading" | "ready" | "error";

const STATUS_STYLES: Record<string, string> = {
  PENDING: "bg-amber-100 text-amber-700 dark:bg-amber-400/10 dark:text-amber-400",
  CONFIRMED:
    "bg-emerald-100 text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-400",
  CANCELLED: "bg-red-100 text-red-700 dark:bg-red-400/10 dark:text-red-400",
  COMPLETED: "bg-zinc-100 text-zinc-600 dark:bg-white/5 dark:text-zinc-400",
};

function formatDateTime(iso: string, timezone: string | null) {
  return formatInZone(iso, timezone, {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function BookingCard({
  booking,
  onCancel,
  cancelling,
}: {
  booking: BookingResponseDto;
  onCancel: (ref: string) => void;
  cancelling: boolean;
}) {
  const { formatPrice } = useCurrency();
  const canCancel =
    booking.status !== "CANCELLED" && booking.status !== "COMPLETED";

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-6"
    >
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-2.5">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-zinc-100 text-zinc-900 dark:bg-white/5 dark:text-accent">
            <Plane className="h-4 w-4 rotate-45" strokeWidth={1.8} />
          </span>
          <div>
            <p className="font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
              {booking.flightNumber}
            </p>
            <p className="font-mono text-xs text-zinc-400 dark:text-zinc-500">
              {booking.bookingReference}
            </p>
          </div>
        </div>
        <span
          className={`rounded-full px-2.5 py-1 text-xs font-medium ${
            STATUS_STYLES[booking.status] ?? STATUS_STYLES.COMPLETED
          }`}
        >
          {booking.status.charAt(0) + booking.status.slice(1).toLowerCase()}
        </span>
      </div>

      <div className="mt-5 flex items-center gap-4">
        <div>
          <p className="font-mono text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {booking.departureAirport}
          </p>
          <p className="mt-0.5 text-xs text-zinc-400 dark:text-zinc-500">
            {formatDateTime(booking.departureTime, booking.departureTimezone)}
          </p>
        </div>
        <div className="flex flex-1 items-center gap-2 text-zinc-300 dark:text-zinc-600">
          <div className="h-px flex-1 bg-zinc-200 dark:bg-white/10" />
          <ArrowRight className="h-4 w-4" strokeWidth={1.5} />
          <div className="h-px flex-1 bg-zinc-200 dark:bg-white/10" />
        </div>
        <div className="text-right">
          <p className="font-mono text-2xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
            {booking.arrivalAirport}
          </p>
          <p className="mt-0.5 text-xs text-zinc-400 dark:text-zinc-500">
            {formatDateTime(booking.arrivalTime, booking.arrivalTimezone)}
          </p>
        </div>
      </div>

      <div className="mt-5 flex items-center justify-between border-t border-zinc-100 pt-4 dark:border-white/5">
        <div className="text-sm text-zinc-500 dark:text-zinc-400">
          {booking.numberOfPassengers}{" "}
          {booking.numberOfPassengers === 1 ? "passenger" : "passengers"} ·{" "}
          {booking.seatClass.toLowerCase()} ·{" "}
          <span className="font-semibold text-zinc-900 dark:text-zinc-100">
            {formatPrice(booking.totalAmount)}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to={`/booking/${booking.bookingReference}`}
            className="rounded-lg border border-zinc-200 px-3 py-1.5 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 dark:border-white/10 dark:text-zinc-300 dark:hover:bg-white/5"
          >
            Details
          </Link>
          {canCancel && (
            <button
              type="button"
              onClick={() => onCancel(booking.bookingReference)}
              disabled={cancelling}
              className="cursor-pointer rounded-lg border border-zinc-200 px-3 py-1.5 text-sm font-medium text-zinc-600 transition-colors hover:border-red-300 hover:text-red-500 disabled:opacity-60 dark:border-white/10 dark:text-zinc-300"
            >
              Cancel
            </button>
          )}
        </div>
      </div>
    </motion.div>
  );
}

export function TripsPage() {
  const { user } = useAuth();
  const [status, setStatus] = useState<Status>("loading");
  const [bookings, setBookings] = useState<BookingResponseDto[]>([]);
  const [cancellingRef, setCancellingRef] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    getBookingsByUser(user.userId)
      .then((data) => {
        setBookings(
          [...data].sort(
            (a, b) =>
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
          ),
        );
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, [user]);

  const handleCancel = async (ref: string) => {
    setCancellingRef(ref);
    try {
      const updated = await cancelBooking(ref);
      setBookings((prev) =>
        prev.map((b) => (b.bookingReference === ref ? updated : b)),
      );
    } catch {
      // leave the booking as-is; a full app would surface a toast here
    } finally {
      setCancellingRef(null);
    }
  };

  return (
    <main className="mx-auto mt-10 max-w-3xl">
      <PageHeader
        eyebrow="Your journeys"
        title="My trips"
        subtitle="View and manage your upcoming and past bookings."
      />

      {status === "loading" && (
        <div className="flex items-center gap-3 text-sm text-zinc-500 dark:text-zinc-400">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
          Loading your trips…
        </div>
      )}

      {status === "error" && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
          Could not load your trips. Please try again later.
        </p>
      )}

      {status === "ready" && bookings.length === 0 && (
        <div className="rounded-2xl border border-dashed border-zinc-200 bg-white/50 p-10 text-center dark:border-white/10 dark:bg-white/[0.02]">
          <p className="text-sm text-zinc-500 dark:text-zinc-400">
            You have no bookings yet.
          </p>
          <Link
            to="/dashboard"
            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-zinc-900 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
          >
            Book a flight
            <ArrowRight className="h-4 w-4" strokeWidth={2} />
          </Link>
        </div>
      )}

      {status === "ready" && bookings.length > 0 && (
        <div className="flex flex-col gap-4">
          {bookings.map((booking) => (
            <BookingCard
              key={booking.id}
              booking={booking}
              onCancel={handleCancel}
              cancelling={cancellingRef === booking.bookingReference}
            />
          ))}
        </div>
      )}
    </main>
  );
}
