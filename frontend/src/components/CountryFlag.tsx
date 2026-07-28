import type { ComponentType } from "react";
import { Globe } from "lucide-react";
import DE from "country-flag-icons/react/3x2/DE";
import ES from "country-flag-icons/react/3x2/ES";
import FR from "country-flag-icons/react/3x2/FR";
import GB from "country-flag-icons/react/3x2/GB";
import IE from "country-flag-icons/react/3x2/IE";
import IT from "country-flag-icons/react/3x2/IT";
import NL from "country-flag-icons/react/3x2/NL";
import PT from "country-flag-icons/react/3x2/PT";

type FlagComponent = ComponentType<{ className?: string; title?: string }>;

/**
 * Official country flags, keyed by the airport's ISO 3166-1 alpha-2 code.
 *
 * Imported one country at a time on purpose. The package's barrel export
 * (`country-flag-icons/react/3x2`) can't be tree-shaken when flags are looked up
 * by a runtime value, so it pulls in all ~250 countries — measured at +54 kB
 * gzipped. Listing the countries the network actually serves costs a fraction of
 * that; add a line here when a route opens to a new country.
 */
const FLAGS: Record<string, FlagComponent> = {
  DE,
  ES,
  FR,
  GB,
  IE,
  IT,
  NL,
  PT,
};

export function CountryFlag({
  countryCode,
  country,
  className = "",
}: {
  countryCode: string | null;
  country: string;
  className?: string;
}) {
  const Flag = countryCode ? FLAGS[countryCode.toUpperCase()] : undefined;

  // Unknown or missing code — fall back to a neutral mark rather than a gap, so
  // an airport added without a flag still renders.
  if (!Flag) {
    return (
      <Globe
        className={`text-zinc-300 dark:text-zinc-700 ${className}`}
        strokeWidth={1}
        aria-label={country}
      />
    );
  }

  return <Flag className={className} title={country} />;
}
