import type { LoyaltySummaryDto } from "../types/flight";

const TIER_LABELS: Record<LoyaltySummaryDto["memberTier"], string> = {
  SILVER: "Silver",
  GOLD: "Gold",
  PLATINUM: "Platinum",
  OBSIDIAN: "Obsidian",
};

export function LoyaltyWidget({ loyalty }: { loyalty: LoyaltySummaryDto }) {
  const nextTierProgress = Math.min(
    100,
    (loyalty.milesBalance /
      (loyalty.milesBalance + loyalty.milesToNextTier)) *
      100,
  );

  return (
    <div className="flex h-full flex-col justify-between rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none">
      <div className="flex items-start justify-between">
        <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
          Loyalty status
        </span>
        <span className="rounded-full border border-zinc-200 px-2.5 py-0.5 text-xs font-medium text-zinc-600 dark:border-white/10 dark:text-zinc-300">
          {TIER_LABELS[loyalty.memberTier]}
        </span>
      </div>

      <div className="mt-4">
        <p className="text-3xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
          {loyalty.milesBalance.toLocaleString()}
        </p>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          miles balance
        </p>
      </div>

      <div className="mt-5">
        <div className="h-1.5 w-full overflow-hidden rounded-full bg-zinc-100 dark:bg-white/10">
          <div
            className="h-full rounded-full bg-accent transition-all duration-700"
            style={{ width: `${nextTierProgress}%` }}
          />
        </div>
        <p className="mt-2 text-xs text-zinc-400 dark:text-zinc-500">
          {loyalty.milesToNextTier.toLocaleString()} miles to next tier ·{" "}
          {loyalty.upcomingTripCount} upcoming trips
        </p>
      </div>
    </div>
  );
}
