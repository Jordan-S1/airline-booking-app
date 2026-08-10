import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import axios from "axios";
import { Check } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { resetPassword } from "../api/auth";

/** Mirrors the backend's @Size(min = 8). */
const MIN_LENGTH = 8;

/**
 * Completes a password reset from a link.
 *
 * <p>The token arrives in the query string. It is a credential, so it is read
 * and posted but never rendered - putting it on screen invites it into a
 * screenshot or a shoulder-surfer's view.
 */
export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const handleSubmit = async () => {
    setError(null);

    if (password.length < MIN_LENGTH) {
      setError(`Password must be at least ${MIN_LENGTH} characters.`);
      return;
    }
    if (password !== confirm) {
      setError("The passwords do not match.");
      return;
    }

    setIsSaving(true);
    try {
      await resetPassword(token, password);
      setDone(true);
    } catch (err) {
      setError(
        axios.isAxiosError(err)
          ? (err.response?.data?.message ??
            "This reset link is invalid or has expired.")
          : "This reset link is invalid or has expired.",
      );
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <main className="mx-auto mt-10 max-w-md">
      <PageHeader
        eyebrow="Account"
        title="Set a new password"
        subtitle="Choose a password you have not used here before."
      />

      <div className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8">
        {!token && (
          <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
            This link is missing its reset token. Request a new one from the
            sign-in screen.
          </p>
        )}

        {token && done && (
          <div className="flex flex-col items-start gap-4">
            <p className="flex items-center gap-2 text-sm font-medium text-emerald-600 dark:text-emerald-400">
              <Check className="h-4 w-4" strokeWidth={2.2} />
              Your password has been changed.
            </p>
            <button
              type="button"
              onClick={() => navigate("/")}
              className="inline-flex cursor-pointer items-center rounded-xl bg-zinc-900 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-zinc-800 pointer-coarse:min-h-11 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
            >
              Go to sign in
            </button>
          </div>
        )}

        {token && !done && (
          <>
            <div className="flex flex-col gap-3">
              <label className="flex flex-col gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
                <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                  New password
                </span>
                <input
                  type="password"
                  value={password}
                  autoComplete="new-password"
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full bg-transparent text-sm font-medium text-zinc-900 outline-none dark:text-zinc-100"
                />
              </label>
              <label className="flex flex-col gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
                <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                  Confirm new password
                </span>
                <input
                  type="password"
                  value={confirm}
                  autoComplete="new-password"
                  onChange={(e) => setConfirm(e.target.value)}
                  className="w-full bg-transparent text-sm font-medium text-zinc-900 outline-none dark:text-zinc-100"
                />
              </label>
            </div>

            {error && (
              <p className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
                {error}
              </p>
            )}

            <button
              type="button"
              onClick={handleSubmit}
              disabled={isSaving || !password || !confirm}
              className="mt-6 flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl bg-zinc-900 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 pointer-coarse:min-h-11 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
            >
              {isSaving ? (
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white dark:border-zinc-900/30 dark:border-t-zinc-900" />
              ) : (
                "Set new password"
              )}
            </button>
          </>
        )}

        <p className="mt-5 text-center text-xs text-zinc-400 dark:text-zinc-500">
          <Link to="/" className="underline underline-offset-2 hover:text-zinc-600 dark:hover:text-zinc-300">
            Back to SkyAir
          </Link>
        </p>
      </div>
    </main>
  );
}
