import type { ComponentType } from "react";
import { Globe } from "lucide-react";
import AE from "country-flag-icons/react/3x2/AE";
import AR from "country-flag-icons/react/3x2/AR";
import AT from "country-flag-icons/react/3x2/AT";
import AU from "country-flag-icons/react/3x2/AU";
import BR from "country-flag-icons/react/3x2/BR";
import CA from "country-flag-icons/react/3x2/CA";
import CH from "country-flag-icons/react/3x2/CH";
import CN from "country-flag-icons/react/3x2/CN";
import DE from "country-flag-icons/react/3x2/DE";
import DK from "country-flag-icons/react/3x2/DK";
import EG from "country-flag-icons/react/3x2/EG";
import ES from "country-flag-icons/react/3x2/ES";
import FR from "country-flag-icons/react/3x2/FR";
import GB from "country-flag-icons/react/3x2/GB";
import HK from "country-flag-icons/react/3x2/HK";
import IE from "country-flag-icons/react/3x2/IE";
import IN from "country-flag-icons/react/3x2/IN";
import IT from "country-flag-icons/react/3x2/IT";
import JP from "country-flag-icons/react/3x2/JP";
import KE from "country-flag-icons/react/3x2/KE";
import KR from "country-flag-icons/react/3x2/KR";
import MX from "country-flag-icons/react/3x2/MX";
import NG from "country-flag-icons/react/3x2/NG";
import NL from "country-flag-icons/react/3x2/NL";
import NZ from "country-flag-icons/react/3x2/NZ";
import PT from "country-flag-icons/react/3x2/PT";
import QA from "country-flag-icons/react/3x2/QA";
import SE from "country-flag-icons/react/3x2/SE";
import SG from "country-flag-icons/react/3x2/SG";
import TH from "country-flag-icons/react/3x2/TH";
import TR from "country-flag-icons/react/3x2/TR";
import US from "country-flag-icons/react/3x2/US";
import ZA from "country-flag-icons/react/3x2/ZA";

type FlagComponent = ComponentType<{ className?: string; title?: string }>;

/**
 * Official country flags, keyed by the airport's ISO 3166-1 alpha-2 code.
 *
 * Imported one country at a time on purpose. The package's barrel export
 * (`country-flag-icons/react/3x2`) can't be tree-shaken when flags are looked up
 * by a runtime value, so it pulls in all ~250 countries — measured at +54 kB
 * gzipped. Listing the countries the network actually serves costs a fraction of
 * that; add a line here when a route opens to a new country.
 *
 * This covers every country the 43 airports sit in. The query that produced the
 * list is `SELECT DISTINCT country_code FROM airports`, which is worth re-running
 * whenever airports are added — an unlisted code falls back to the globe mark
 * rather than breaking, so a gap is easy to miss.
 */
const FLAGS: Record<string, FlagComponent> = {
  AE,
  AR,
  AT,
  AU,
  BR,
  CA,
  CH,
  CN,
  DE,
  DK,
  EG,
  ES,
  FR,
  GB,
  HK,
  IE,
  IN,
  IT,
  JP,
  KE,
  KR,
  MX,
  NG,
  NL,
  NZ,
  PT,
  QA,
  SE,
  SG,
  TH,
  TR,
  US,
  ZA,
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
