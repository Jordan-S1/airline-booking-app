import { useState } from "react";
import axios from "axios";
import * as Dialog from "@radix-ui/react-dialog";
import { useReturnFocus } from "../lib/useReturnFocus";
import { forgotPassword } from "../api/auth";
import { Eye, EyeOff, Lock, Mail, Phone, User, X } from "lucide-react";
import { useAuth } from "../lib/auth";

type Mode = "login" | "register";

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data;
    const validationErrors = data?.validationErrors as
      | Record<string, string>
      | undefined;
    if (validationErrors && Object.keys(validationErrors).length > 0) {
      return Object.values(validationErrors).join(" ");
    }
    return data?.message ?? data?.error ?? fallback;
  }
  return fallback;
}

const MIN_PASSWORD_LENGTH = 8;

function getPasswordStrength(password: string): {
  score: number;
  label: string;
} {
  if (password.length === 0) return { score: 0, label: "" };
  if (password.length < MIN_PASSWORD_LENGTH) {
    return { score: 0, label: "Too short" };
  }

  let score = 1;
  if (password.length >= 12) score++;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++;
  if (/\d/.test(password) || /[^A-Za-z0-9]/.test(password)) score++;

  const labels = ["", "Weak", "Fair", "Good", "Strong"];
  return { score, label: labels[score] };
}

function PasswordStrengthMeter({ password }: { password: string }) {
  if (password.length === 0) return null;
  const { score, label } = getPasswordStrength(password);
  const barColor =
    score <= 1
      ? "bg-red-400"
      : score === 2
        ? "bg-amber-400"
        : score === 3
          ? "bg-yellow-400"
          : "bg-emerald-400";

  return (
    <div className="flex items-center gap-2 px-1">
      <div className="flex flex-1 gap-1">
        {[1, 2, 3, 4].map((segment) => (
          <span
            key={segment}
            className={`h-1 flex-1 rounded-full transition-colors ${
              segment <= score ? barColor : "bg-zinc-200 dark:bg-white/10"
            }`}
          />
        ))}
      </div>
      <span className="text-xs text-zinc-400 dark:text-zinc-500">
        {label}
      </span>
    </div>
  );
}

function TextField({
  label,
  icon,
  type = "text",
  value,
  onChange,
  required = true,
  minLength,
  toggleablePassword = false,
}: {
  label: string;
  icon: React.ReactNode;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  minLength?: number;
  toggleablePassword?: boolean;
}) {
  const [revealed, setRevealed] = useState(false);
  const resolvedType = toggleablePassword && revealed ? "text" : type;

  return (
    <label className="flex flex-col gap-1 rounded-xl border border-zinc-200 bg-white px-4 py-2 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
      <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
        {label}
      </span>
      <div className="flex items-center gap-2.5">
        <span className="text-zinc-300 dark:text-zinc-600">{icon}</span>
        <input
          type={resolvedType}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          required={required}
          minLength={minLength}
          className="w-full bg-transparent text-sm font-medium text-zinc-900 outline-none dark:text-zinc-100"
        />
        {toggleablePassword && (
          <button
            type="button"
            onClick={() => setRevealed((prev) => !prev)}
            aria-label={revealed ? "Hide password" : "Show password"}
            className="cursor-pointer text-zinc-300 transition-colors hover:text-zinc-500 dark:text-zinc-600 dark:hover:text-zinc-400"
          >
            {revealed ? (
              <EyeOff className="h-4 w-4" />
            ) : (
              <Eye className="h-4 w-4" />
            )}
          </button>
        )}
      </div>
    </label>
  );
}

export function AuthModal({ onClose }: { onClose: () => void }) {
  useReturnFocus();
  const { login, register } = useAuth();
  const [mode, setMode] = useState<Mode>("login");
  /** Set once a reset has been requested, so the reply is shown in place. */
  const [resetRequested, setResetRequested] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      if (mode === "login") {
        await login({ email, password });
      } else {
        await register({
          firstName,
          lastName,
          email,
          password,
          phoneNumber: phoneNumber || null,
        });
      }
      onClose();
    } catch (err) {
      setError(
        extractErrorMessage(
          err,
          mode === "login"
            ? "Invalid email or password."
            : "Could not create account.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog.Root open onOpenChange={(next) => { if (!next) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Overlay className="overlay fixed inset-0 z-50 bg-black/40 backdrop-blur-sm" />
        <Dialog.Content
          className="pop fixed inset-0 z-50 m-auto h-fit max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-md overflow-y-auto rounded-2xl border border-zinc-200 bg-white p-5 shadow-xl focus:outline-none dark:border-white/10 dark:bg-obsidian-raised sm:p-6"
        >
          {/* Ambient accent glow, consistent with the dashboard background */}
          <div
            aria-hidden
            className="pointer-events-none absolute inset-x-0 -top-24 h-48 bg-[radial-gradient(circle_at_50%_0%,rgba(6,182,212,0.16),transparent_70%)]"
          />

          <Dialog.Close
            aria-label="Close"
            className="absolute right-5 top-5 flex h-7 w-7 cursor-pointer items-center justify-center rounded-full text-zinc-400 transition-colors hover:bg-zinc-100 hover:text-zinc-600 pointer-coarse:h-11 pointer-coarse:w-11 dark:text-zinc-500 dark:hover:bg-white/5 dark:hover:text-zinc-300"
          >
            <X className="h-4 w-4" />
          </Dialog.Close>

          <div className="mb-4 flex flex-col items-center text-center">
            {/* Matches the navbar mark: the aircraft on its own, no badge. */}
            <img
              src="/airplane.png"
              alt=""
              className="mb-2 h-8 w-8 object-contain"
            />
            <Dialog.Title className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
              {mode === "login" ? "Welcome back" : "Create your account"}
            </Dialog.Title>
            <p className="mt-0.5 text-sm text-zinc-500 dark:text-zinc-400">
              {mode === "login"
                ? "Sign in to manage your bookings and trips."
                : "Join SkyAir to start booking flights."}
            </p>
          </div>

          <div className="mb-4 flex gap-1 rounded-xl border border-zinc-200 p-1 dark:border-white/10">
            {(["login", "register"] as Mode[]).map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => {
                  setMode(m);
                  setError(null);
                }}
                className={`flex-1 cursor-pointer rounded-lg py-1.5 text-sm font-medium transition-colors ${
                  mode === m
                    ? "bg-zinc-900 text-white dark:bg-white dark:text-zinc-900"
                    : "text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
                }`}
              >
                {m === "login" ? "Sign in" : "Register"}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-2.5">
            {mode === "register" && (
              <div className="flex gap-3">
                <TextField
                  label="First name"
                  icon={<User className="h-4 w-4" />}
                  value={firstName}
                  onChange={setFirstName}
                />
                <TextField
                  label="Last name"
                  icon={<User className="h-4 w-4" />}
                  value={lastName}
                  onChange={setLastName}
                />
              </div>
            )}

            <TextField
              label="Email"
              icon={<Mail className="h-4 w-4" />}
              type="email"
              value={email}
              onChange={setEmail}
            />
            <TextField
              label="Password"
              icon={<Lock className="h-4 w-4" />}
              type="password"
              toggleablePassword
              minLength={mode === "register" ? MIN_PASSWORD_LENGTH : undefined}
              value={password}
              onChange={setPassword}
            />
            {mode === "register" && (
              <PasswordStrengthMeter password={password} />
            )}

            {mode === "login" && !resetRequested && (
              <button
                type="button"
                onClick={async () => {
                  // Fires whether or not the address is registered; the API
                  // answers identically either way, so this must not report
                  // "no such account" and reveal what the API withheld.
                  setResetRequested(true);
                  try {
                    await forgotPassword(email);
                  } catch {
                    // Deliberately swallowed — see above.
                  }
                }}
                disabled={!email}
                className="-mt-1 self-end text-xs text-zinc-500 underline underline-offset-2 transition-colors hover:text-zinc-900 disabled:cursor-not-allowed disabled:no-underline disabled:opacity-50 pointer-coarse:min-h-11 dark:text-zinc-400 dark:hover:text-zinc-100 cursor-pointer"
              >
                Forgot password?
              </button>
            )}

            {mode === "login" && resetRequested && (
              <p className="rounded-xl border border-zinc-200 bg-zinc-50 px-3 py-2 text-xs text-zinc-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-zinc-300">
                {/* Deliberately vague about delivery: the backend emails the
                    link when SMTP is configured and logs it otherwise, and the
                    browser has no way to know which. Naming one would be wrong
                    half the time. */}
                If that address has an account, a reset link is on its way.
                Check your inbox.
              </p>
            )}

            {mode === "register" && (
              <TextField
                label="Phone (optional)"
                icon={<Phone className="h-4 w-4" />}
                type="tel"
                value={phoneNumber}
                onChange={setPhoneNumber}
                required={false}
              />
            )}

            {error && (
              <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="mt-1 flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl bg-zinc-900 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
            >
              {isSubmitting ? (
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white dark:border-zinc-900/30 dark:border-t-zinc-900" />
              ) : mode === "login" ? (
                "Sign in"
              ) : (
                "Create account"
              )}
            </button>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
