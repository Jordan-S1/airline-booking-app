import { useState } from "react";
import axios from "axios";
import { Check, Eye, EyeOff } from "lucide-react";
import { changePassword } from "../api/users";

/** Mirrors the backend's @Size(min = 8) so the rule is stated in both places. */
const MIN_LENGTH = 8;

function PasswordField({
  label,
  value,
  onChange,
  autoComplete,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete: string;
}) {
  const [revealed, setRevealed] = useState(false);

  return (
    <label className="flex flex-col gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
      <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
        {label}
      </span>
      <span className="flex items-center gap-2">
        <input
          type={revealed ? "text" : "password"}
          value={value}
          autoComplete={autoComplete}
          onChange={(e) => onChange(e.target.value)}
          className="w-full min-w-0 bg-transparent text-sm font-medium text-zinc-900 outline-none dark:text-zinc-100"
        />
        <button
          type="button"
          onClick={() => setRevealed((r) => !r)}
          aria-label={revealed ? "Hide password" : "Show password"}
          className="flex shrink-0 cursor-pointer items-center justify-center text-zinc-300 transition-colors hover:text-zinc-500 pointer-coarse:h-11 pointer-coarse:w-11 dark:text-zinc-600 dark:hover:text-zinc-400"
        >
          {revealed ? (
            <EyeOff className="h-4 w-4" strokeWidth={1.8} />
          ) : (
            <Eye className="h-4 w-4" strokeWidth={1.8} />
          )}
        </button>
      </span>
    </label>
  );
}

/**
 * Lets a signed-in user change their own password.
 *
 * <p>There is no reset-by-email flow, so this is the only way a password can
 * change once set. The current password is required by the API, which is why
 * it is asked for here rather than trusting the session.
 */
export function ChangePasswordCard({ userId }: { userId: number }) {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [changed, setChanged] = useState(false);

  const reset = () => {
    setCurrent("");
    setNext("");
    setConfirm("");
  };

  const handleSubmit = async () => {
    setError(null);
    setChanged(false);

    // Checked here as well as server-side so the mismatch is caught without a
    // round trip; the confirmation field exists only on the client.
    if (next.length < MIN_LENGTH) {
      setError(`New password must be at least ${MIN_LENGTH} characters.`);
      return;
    }
    if (next !== confirm) {
      setError("The new passwords do not match.");
      return;
    }
    if (next === current) {
      setError("New password must be different from the current one.");
      return;
    }

    setIsSaving(true);
    try {
      await changePassword(userId, {
        currentPassword: current,
        newPassword: next,
      });
      reset();
      setChanged(true);
    } catch (err) {
      // The API answers a wrong current password with 400 and a usable
      // message; anything else falls back to something non-specific.
      const message = axios.isAxiosError(err)
        ? (err.response?.data?.message ?? "Could not change your password.")
        : "Could not change your password.";
      setError(message);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="mt-5 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8">
      <div className="mb-4">
        <h2 className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
          Password
        </h2>
        <p className="mt-0.5 text-sm text-zinc-500 dark:text-zinc-400">
          Changing it signs you out of nothing else - existing sessions keep
          working until their token expires.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <PasswordField
            label="Current password"
            value={current}
            onChange={setCurrent}
            autoComplete="current-password"
          />
        </div>
        <PasswordField
          label="New password"
          value={next}
          onChange={setNext}
          autoComplete="new-password"
        />
        <PasswordField
          label="Confirm new password"
          value={confirm}
          onChange={setConfirm}
          autoComplete="new-password"
        />
      </div>

      {error && (
        <p className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
          {error}
        </p>
      )}

      <div className="mt-6 flex items-center gap-3">
        <button
          type="button"
          onClick={handleSubmit}
          disabled={isSaving || !current || !next || !confirm}
          className="flex cursor-pointer items-center justify-center gap-2 rounded-xl bg-zinc-900 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 pointer-coarse:min-h-11 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
        >
          {isSaving ? (
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white dark:border-zinc-900/30 dark:border-t-zinc-900" />
          ) : (
            "Change password"
          )}
        </button>
        {changed && (
          <span className="flex items-center gap-1.5 text-sm font-medium text-emerald-600 dark:text-emerald-400">
            <Check className="h-4 w-4" strokeWidth={2.2} />
            Password changed
          </span>
        )}
      </div>
    </div>
  );
}
