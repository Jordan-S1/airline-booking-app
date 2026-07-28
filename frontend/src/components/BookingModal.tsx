import { useState } from "react";
import { Link } from "react-router-dom";
import axios from "axios";
import { AnimatePresence, motion } from "framer-motion";
import { Check, X } from "lucide-react";
import { useAuth } from "../lib/auth";
import { useCurrency } from "../lib/currency";
import { createBooking, confirmBooking } from "../api/bookings";
import { createPayment } from "../api/payments";
import { AuthModal } from "./AuthModal";
import { SelectField, type SelectOption } from "./SelectField";
import type { FlightSearchResponseDto } from "../types/flight";
import type {
  BookingResponseDto,
  Gender,
  PassengerRequestDto,
  PaymentMethod,
} from "../types/booking";

const GENDERS: SelectOption<Gender>[] = [
  { value: "MALE", label: "Male" },
  { value: "FEMALE", label: "Female" },
  { value: "OTHER", label: "Other" },
];

const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "CREDIT_CARD", label: "Credit card" },
  { value: "DEBIT_CARD", label: "Debit card" },
  { value: "PAYPAL", label: "PayPal" },
  { value: "BANK_TRANSFER", label: "Bank transfer" },
];

function emptyPassenger(): PassengerRequestDto {
  return {
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    gender: "MALE",
    passportNumber: "",
    nationality: "",
  };
}

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    return err.response?.data?.message ?? err.response?.data?.error ?? fallback;
  }
  return fallback;
}

interface BookingModalProps {
  /** One flight for a one-way trip; one per leg for round trips and multi-city. */
  flights: FlightSearchResponseDto[];
  passengerCount: number;
  seatClass: string;
  onClose: () => void;
}

type Step = "passengers" | "payment" | "confirmation";

export function BookingModal({
  flights,
  passengerCount,
  seatClass,
  onClose,
}: BookingModalProps) {
  const { isAuthenticated, user } = useAuth();
  const { formatPrice } = useCurrency();
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [step, setStep] = useState<Step>("passengers");
  const [passengers, setPassengers] = useState<PassengerRequestDto[]>(() =>
    Array.from({ length: passengerCount }, emptyPassenger),
  );
  const [paymentMethod, setPaymentMethod] =
    useState<PaymentMethod>("CREDIT_CARD");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmedBookings, setConfirmedBookings] = useState<
    BookingResponseDto[]
  >([]);

  const isMultiLeg = flights.length > 1;
  const legTotal = flights.reduce((sum, f) => sum + f.price, 0);
  const totalAmount = legTotal * passengerCount;

  const updatePassenger = (
    index: number,
    field: keyof PassengerRequestDto,
    value: string,
  ) => {
    setPassengers((prev) =>
      prev.map((p, i) => (i === index ? { ...p, [field]: value } : p)),
    );
  };

  /**
   * Books each leg in turn. The API models one booking per flight, so a
   * multi-leg itinerary produces one reference per leg rather than a single
   * PNR. If a later leg fails, earlier legs stay booked — the error names the
   * leg so the user knows exactly where it stopped.
   */
  const handleConfirmAndPay = async () => {
    if (!user) return;
    setIsSubmitting(true);
    setError(null);

    const completed: BookingResponseDto[] = [];

    try {
      for (const flight of flights) {
        const booking = await createBooking(user.userId, {
          flightId: flight.id,
          seatClass,
          passengers,
        });
        await createPayment({ bookingId: booking.id, paymentMethod });
        completed.push(await confirmBooking(booking.bookingReference));
      }
      setConfirmedBookings(completed);
      setStep("confirmation");
    } catch (err) {
      const failedLeg = completed.length + 1;
      const base = extractErrorMessage(
        err,
        "Something went wrong with your booking.",
      );
      setError(
        isMultiLeg
          ? `${base} (failed on leg ${failedLeg} of ${flights.length}${
              completed.length > 0
                ? `; legs 1-${completed.length} were booked`
                : ""
            })`
          : base,
      );
      if (completed.length > 0) setConfirmedBookings(completed);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
        className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/40 px-4 py-8 backdrop-blur-sm"
      >
        <motion.div
          initial={{ opacity: 0, y: 12, scale: 0.98 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 12, scale: 0.98 }}
          transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
          onClick={(e) => e.stopPropagation()}
          className="w-full max-w-lg rounded-2xl border border-zinc-200 bg-white p-6 shadow-xl dark:border-white/10 dark:bg-obsidian-raised sm:p-8"
        >
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
                {step === "confirmation"
                  ? isMultiLeg
                    ? "Itinerary confirmed"
                    : "Booking confirmed"
                  : isMultiLeg
                    ? `Book itinerary · ${flights.length} legs`
                    : "Book flight"}
              </h2>
              <p className="mt-0.5 font-mono text-sm text-zinc-500 dark:text-zinc-400">
                {flights[0].departureAirport}
                {flights.map((f) => ` → ${f.arrivalAirport}`).join("")}
              </p>
            </div>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close"
              className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-zinc-400 transition-colors hover:bg-zinc-100 hover:text-zinc-600 dark:text-zinc-500 dark:hover:bg-white/5 dark:hover:text-zinc-300"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          {!isAuthenticated && (
            <div className="flex flex-col items-start gap-3 rounded-xl border border-zinc-200 bg-zinc-50 p-4 dark:border-white/10 dark:bg-white/[0.03]">
              <p className="text-sm text-zinc-600 dark:text-zinc-300">
                Sign in to continue booking this flight.
              </p>
              <button
                type="button"
                onClick={() => setIsAuthModalOpen(true)}
                className="rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
              >
                Sign in
              </button>
            </div>
          )}

          {isAuthenticated && step === "passengers" && (
            <div className="flex flex-col gap-5">
              {passengers.map((passenger, index) => (
                <div
                  key={index}
                  className="rounded-xl border border-zinc-200 p-4 dark:border-white/10"
                >
                  <p className="mb-3 text-xs font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                    Passenger {index + 1}
                  </p>
                  <div className="grid grid-cols-2 gap-3">
                    <input
                      value={passenger.firstName}
                      onChange={(e) =>
                        updatePassenger(index, "firstName", e.target.value)
                      }
                      placeholder="First name"
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:text-zinc-100"
                    />
                    <input
                      value={passenger.lastName}
                      onChange={(e) =>
                        updatePassenger(index, "lastName", e.target.value)
                      }
                      placeholder="Last name"
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:text-zinc-100"
                    />
                    <label className="flex flex-col gap-1 rounded-lg border border-zinc-200 bg-white px-3 py-2 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
                      <span className="text-[10px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                        Date of birth
                      </span>
                      <input
                        type="date"
                        value={passenger.dateOfBirth}
                        onChange={(e) =>
                          updatePassenger(index, "dateOfBirth", e.target.value)
                        }
                        className="w-full bg-transparent text-sm font-medium text-zinc-900 outline-none [color-scheme:light] dark:text-zinc-100 dark:[color-scheme:dark]"
                      />
                    </label>
                    <SelectField
                      label="Gender"
                      value={passenger.gender}
                      options={GENDERS}
                      onChange={(gender) =>
                        updatePassenger(index, "gender", gender)
                      }
                    />
                    <input
                      value={passenger.passportNumber}
                      onChange={(e) =>
                        updatePassenger(index, "passportNumber", e.target.value)
                      }
                      placeholder="Passport number"
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:text-zinc-100"
                    />
                    <input
                      value={passenger.nationality}
                      onChange={(e) =>
                        updatePassenger(index, "nationality", e.target.value)
                      }
                      placeholder="Nationality"
                      className="w-full rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:text-zinc-100"
                    />
                  </div>
                </div>
              ))}

              <button
                type="button"
                onClick={() => setStep("payment")}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-zinc-900 py-3 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
              >
                Continue to payment
              </button>
            </div>
          )}

          {isAuthenticated && step === "payment" && (
            <div className="flex flex-col gap-5">
              <div className="rounded-xl border border-zinc-200 p-4 dark:border-white/10">
                {flights.map((flight, index) => (
                  <div
                    key={flight.id}
                    className="flex items-center justify-between border-b border-zinc-100 py-2 text-sm first:pt-0 last:border-0 last:pb-0 dark:border-white/5"
                  >
                    <span className="text-zinc-500 dark:text-zinc-400">
                      {isMultiLeg && (
                        <span className="mr-1.5 font-medium text-zinc-400 dark:text-zinc-500">
                          Leg {index + 1}
                        </span>
                      )}
                      <span className="font-mono">
                        {flight.departureAirport} → {flight.arrivalAirport}
                      </span>{" "}
                      · {passengerCount} × {formatPrice(flight.price)}
                    </span>
                    <span className="font-medium text-zinc-900 dark:text-zinc-100">
                      {formatPrice(flight.price * passengerCount)}
                    </span>
                  </div>
                ))}
                <div className="mt-3 flex items-center justify-between border-t border-zinc-200 pt-3 text-sm dark:border-white/10">
                  <span className="text-zinc-500 dark:text-zinc-400">
                    Total · {seatClass.toLowerCase()}
                  </span>
                  <span className="font-semibold text-zinc-900 dark:text-zinc-100">
                    {formatPrice(totalAmount)}
                  </span>
                </div>
              </div>

              <div>
                <p className="mb-2.5 text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                  Payment method
                </p>
                <div className="grid grid-cols-2 gap-2">
                  {PAYMENT_METHODS.map((method) => (
                    <button
                      key={method.value}
                      type="button"
                      onClick={() => setPaymentMethod(method.value)}
                      className={`rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
                        paymentMethod === method.value
                          ? "border-accent/40 bg-accent/10 text-accent"
                          : "border-zinc-200 text-zinc-600 hover:border-zinc-300 dark:border-white/10 dark:text-zinc-400 dark:hover:border-white/20"
                      }`}
                    >
                      {method.label}
                    </button>
                  ))}
                </div>
              </div>

              {error && (
                <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
                  {error}
                </p>
              )}

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setStep("passengers")}
                  className="rounded-xl border border-zinc-200 px-4 py-3 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 dark:border-white/10 dark:text-zinc-300 dark:hover:bg-white/5"
                >
                  Back
                </button>
                <button
                  type="button"
                  onClick={handleConfirmAndPay}
                  disabled={isSubmitting}
                  className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-zinc-900 py-3 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
                >
                  {isSubmitting ? (
                    <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white dark:border-zinc-900/30 dark:border-t-zinc-900" />
                  ) : (
                    `Pay ${formatPrice(totalAmount)}`
                  )}
                </button>
              </div>
            </div>
          )}

          {step === "confirmation" && confirmedBookings.length > 0 && (
            <div className="flex flex-col items-center gap-4 py-4 text-center">
              <span className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-400/10 dark:text-emerald-400">
                <Check className="h-6 w-6" strokeWidth={2.2} />
              </span>
              <div className="w-full">
                {confirmedBookings.length === 1 ? (
                  <p className="font-mono text-lg font-semibold text-zinc-900 dark:text-zinc-100">
                    {confirmedBookings[0].bookingReference}
                  </p>
                ) : (
                  <div className="flex flex-col gap-1.5">
                    {confirmedBookings.map((booking, index) => (
                      <div
                        key={booking.id}
                        className="flex items-center justify-between rounded-lg border border-zinc-200 px-3 py-2 text-left dark:border-white/10"
                      >
                        <span className="text-xs text-zinc-400 dark:text-zinc-500">
                          Leg {index + 1} ·{" "}
                          <span className="font-mono">
                            {booking.departureAirport} → {booking.arrivalAirport}
                          </span>
                        </span>
                        <span className="font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                          {booking.bookingReference}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
                <p className="mt-3 text-sm text-zinc-500 dark:text-zinc-400">
                  {confirmedBookings.length === 1
                    ? "Your booking is confirmed."
                    : `All ${confirmedBookings.length} legs are confirmed — each leg has its own reference.`}{" "}
                  A copy has been sent to {confirmedBookings[0].userEmail}.
                </p>
              </div>
              <div className="flex w-full gap-3">
                <Link
                  to={
                    confirmedBookings.length === 1
                      ? `/booking/${confirmedBookings[0].bookingReference}`
                      : "/trips"
                  }
                  onClick={onClose}
                  className="flex-1 rounded-xl bg-zinc-900 py-3 text-center text-sm font-semibold text-white transition-colors hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
                >
                  {confirmedBookings.length === 1
                    ? "View booking"
                    : "View my trips"}
                </Link>
                <button
                  type="button"
                  onClick={onClose}
                  className="flex-1 cursor-pointer rounded-xl border border-zinc-200 py-3 text-sm font-semibold text-zinc-700 transition-colors hover:bg-zinc-100 dark:border-white/10 dark:text-zinc-200 dark:hover:bg-white/5"
                >
                  Done
                </button>
              </div>
            </div>
          )}
        </motion.div>
      </motion.div>

      {isAuthModalOpen && (
        <AuthModal onClose={() => setIsAuthModalOpen(false)} />
      )}
    </AnimatePresence>
  );
}
