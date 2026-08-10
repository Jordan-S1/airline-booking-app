import { useEffect, useState } from "react";
import axios from "axios";
import { Check } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { ChangePasswordCard } from "../components/ChangePasswordCard";
import { SelectField } from "../components/SelectField";
import { useAuth } from "../lib/auth";
import { useCurrency } from "../lib/currency";
import { getUser, updateUser } from "../api/users";
import type { UserResponseDto, UserUpdateRequestDto } from "../types/user";

type Status = "loading" | "ready" | "error";

function Field({
  label,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}) {
  return (
    <label className="flex flex-col gap-1.5 rounded-xl border border-zinc-200 bg-white px-4 py-2.5 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
      <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
        {label}
      </span>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full bg-transparent text-sm font-medium text-zinc-900 outline-none placeholder:text-zinc-300 dark:text-zinc-100 dark:placeholder:text-zinc-700"
      />
    </label>
  );
}

export function ProfilePage() {
  const { user, updateStoredUser } = useAuth();
  const { currencies, setSelectedCode } = useCurrency();
  const [status, setStatus] = useState<Status>("loading");
  const [form, setForm] = useState<UserResponseDto | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    getUser(user.userId)
      .then((data) => {
        setForm(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, [user]);

  const set = (field: keyof UserResponseDto, value: string) => {
    setForm((prev) => (prev ? { ...prev, [field]: value } : prev));
    setSaved(false);
  };

  const handleSave = async () => {
    if (!user || !form) return;
    setIsSaving(true);
    setError(null);
    setSaved(false);

    const payload: UserUpdateRequestDto = {
      firstName: form.firstName,
      lastName: form.lastName,
      phoneNumber: form.phoneNumber,
      address: form.address,
      city: form.city,
      country: form.country,
      postalCode: form.postalCode,
      preferredCurrency: form.preferredCurrency,
    };

    try {
      const updated = await updateUser(user.userId, payload);
      updateStoredUser({
        firstName: updated.firstName,
        lastName: updated.lastName,
        preferredCurrency: updated.preferredCurrency,
      });
      setSelectedCode(updated.preferredCurrency);
      setSaved(true);
    } catch (err) {
      setError(
        axios.isAxiosError(err)
          ? (err.response?.data?.message ?? "Could not save changes.")
          : "Could not save changes.",
      );
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <main className="mx-auto mt-10 max-w-3xl">
      <PageHeader
        eyebrow="Account"
        title="Profile"
        subtitle="Manage your personal details and display preferences."
      />

      {status === "loading" && (
        <div className="flex items-center gap-3 text-sm text-zinc-500 dark:text-zinc-400">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
          Loading your profile…
        </div>
      )}

      {status === "error" && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
          Could not load your profile. Please try again later.
        </p>
      )}

      {status === "ready" && form && (
        <div className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none sm:p-8">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field
              label="First name"
              value={form.firstName}
              onChange={(v) => set("firstName", v)}
            />
            <Field
              label="Last name"
              value={form.lastName}
              onChange={(v) => set("lastName", v)}
            />
            <label className="flex flex-col gap-1.5 rounded-xl border border-zinc-200 bg-zinc-50 px-4 py-2.5 dark:border-white/10 dark:bg-white/[0.02] sm:col-span-2">
              <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                Email
              </span>
              <span className="text-sm font-medium text-zinc-500 dark:text-zinc-400">
                {form.email}
              </span>
            </label>
            <Field
              label="Phone"
              value={form.phoneNumber ?? ""}
              onChange={(v) => set("phoneNumber", v)}
            />
            <Field
              label="Country"
              value={form.country ?? ""}
              onChange={(v) => set("country", v)}
              placeholder="Ireland"
            />
            <Field
              label="Address"
              value={form.address ?? ""}
              onChange={(v) => set("address", v)}
            />
            <Field
              label="City"
              value={form.city ?? ""}
              onChange={(v) => set("city", v)}
            />
            <Field
              label="Postal code"
              value={form.postalCode ?? ""}
              onChange={(v) => set("postalCode", v)}
            />
            <SelectField
              label="Display currency"
              value={form.preferredCurrency}
              options={
                currencies.length > 0
                  ? currencies.map((c) => ({
                      value: c.code,
                      label: `${c.code} - ${c.name}`,
                      hint: c.symbol,
                    }))
                  : [
                      {
                        value: form.preferredCurrency,
                        label: form.preferredCurrency,
                      },
                    ]
              }
              onChange={(code) => set("preferredCurrency", code)}
            />
          </div>

          {error && (
            <p className="mt-4 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
              {error}
            </p>
          )}

          <div className="mt-6 flex items-center gap-3">
            <button
              type="button"
              onClick={handleSave}
              disabled={isSaving}
              className="flex cursor-pointer items-center justify-center gap-2 rounded-xl bg-zinc-900 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 pointer-coarse:min-h-11 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
            >
              {isSaving ? (
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white dark:border-zinc-900/30 dark:border-t-zinc-900" />
              ) : (
                "Save changes"
              )}
            </button>
            {saved && (
              <span className="flex items-center gap-1.5 text-sm font-medium text-emerald-600 dark:text-emerald-400">
                <Check className="h-4 w-4" strokeWidth={2.2} />
                Saved
              </span>
            )}
          </div>
        </div>
      )}

      {status === "ready" && user && <ChangePasswordCard userId={user.userId} />}
    </main>
  );
}
