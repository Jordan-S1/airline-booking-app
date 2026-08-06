import * as Dialog from "@radix-ui/react-dialog";
import { useReturnFocus } from "../../lib/useReturnFocus";
import { X } from "lucide-react";
import type { FormEvent, ReactNode } from "react";

/**
 * Shell for the admin create/edit dialogs. Holds the chrome — backdrop, title,
 * error banner, submit/cancel — so each entity form is only its own fields.
 */
export function AdminFormModal({
  title,
  subtitle,
  error,
  submitLabel,
  isSubmitting,
  onSubmit,
  onClose,
  children,
}: {
  title: string;
  subtitle?: string;
  error: string | null;
  submitLabel: string;
  isSubmitting: boolean;
  onSubmit: () => void;
  onClose: () => void;
  children: ReactNode;
}) {
  useReturnFocus();

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    // Radix owns the open state; this component is only rendered while it
    // should be visible, so `open` is constant and closing is delegated
    // upward. onOpenChange covers Escape and outside-click alike.
    <Dialog.Root open onOpenChange={(next) => { if (!next) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Overlay className="overlay fixed inset-0 z-50 bg-black/40 backdrop-blur-sm" />
        <Dialog.Content
          className="pop fixed inset-0 z-50 m-auto h-fit max-h-[calc(100dvh-5rem)] w-[calc(100%-2rem)] max-w-xl overflow-y-auto rounded-2xl border border-zinc-200 bg-white p-6 shadow-xl focus:outline-none dark:border-white/10 dark:bg-obsidian-raised sm:p-8"
        >
          <div className="mb-6 flex items-start justify-between gap-4">
            <div>
              <Dialog.Title className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100">
                {title}
              </Dialog.Title>
              {subtitle && (
                <Dialog.Description className="mt-0.5 text-sm text-zinc-500 dark:text-zinc-400">
                  {subtitle}
                </Dialog.Description>
              )}
            </div>
            <Dialog.Close
              aria-label="Close"
              className="flex h-7 w-7 shrink-0 cursor-pointer items-center justify-center rounded-full text-zinc-400 transition-colors hover:bg-zinc-100 hover:text-zinc-600 pointer-coarse:h-11 pointer-coarse:w-11 dark:text-zinc-500 dark:hover:bg-white/5 dark:hover:text-zinc-300"
            >
              <X className="h-4 w-4" />
            </Dialog.Close>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-3">
            {children}

            {error && (
              <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-400/20 dark:bg-red-400/10 dark:text-red-400">
                {error}
              </p>
            )}

            <div className="mt-2 flex gap-3">
              <button
                type="button"
                onClick={onClose}
                className="cursor-pointer rounded-xl border border-zinc-200 px-5 py-2.5 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 dark:border-white/10 dark:text-zinc-300 dark:hover:bg-white/5"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-xl bg-zinc-900 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200"
              >
                {isSubmitting && (
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white dark:border-zinc-900/30 dark:border-t-zinc-900" />
                )}
                {isSubmitting ? "Saving…" : submitLabel}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

/** A labelled text input matching the app's field styling. */
export function Field({
  label,
  value,
  onChange,
  placeholder,
  type = "text",
  disabled = false,
  hint,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  type?: string;
  disabled?: boolean;
  hint?: string;
}) {
  return (
    <label className="flex flex-col gap-1 rounded-xl border border-zinc-200 bg-white px-4 py-2 transition-colors focus-within:border-zinc-400 dark:border-white/10 dark:bg-white/[0.03] dark:focus-within:border-white/30">
      <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
        {label}
        {hint && (
          <span className="ml-1.5 normal-case tracking-normal text-zinc-300 dark:text-zinc-600">
            {hint}
          </span>
        )}
      </span>
      <input
        type={type}
        value={value}
        disabled={disabled}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="w-full bg-transparent text-sm font-medium text-zinc-900 outline-none placeholder:text-zinc-300 disabled:text-zinc-400 dark:text-zinc-100 dark:placeholder:text-zinc-700 dark:disabled:text-zinc-500"
      />
    </label>
  );
}
