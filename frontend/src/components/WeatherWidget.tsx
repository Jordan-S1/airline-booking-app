import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import {
  Cloud,
  CloudDrizzle,
  CloudFog,
  CloudLightning,
  CloudOff,
  CloudRain,
  CloudSnow,
  CloudSun,
  Moon,
  Sun,
  type LucideIcon,
} from "lucide-react";
import { getCityWeather, type CityWeather } from "../api/weather";

type Status = "loading" | "error" | "success";

/** A settled fetch, tagged with the city it was made for. `null` means it failed. */
interface WeatherResult {
  city: string;
  weather: CityWeather | null;
}

/**
 * The icon set, keyed by condition. Kept as a static map so the icon rendered
 * below is only ever looked up, never produced by a call — a component whose
 * identity came out of a function would remount on every render.
 */
const WEATHER_ICONS = {
  clearDay: Sun,
  clearNight: Moon,
  partlyCloudyDay: CloudSun,
  cloudy: Cloud,
  fog: CloudFog,
  drizzle: CloudDrizzle,
  rain: CloudRain,
  snow: CloudSnow,
  thunderstorm: CloudLightning,
  unavailable: CloudOff,
} satisfies Record<string, LucideIcon>;

type WeatherIconKey = keyof typeof WEATHER_ICONS;

/**
 * Maps a WMO interpretation code to a key. Clear/partly-cloudy states swap to
 * a night variant after dark; precipitation looks the same either way.
 */
function weatherIconKey(code: number, isDay: boolean): WeatherIconKey {
  if (code === 0) return isDay ? "clearDay" : "clearNight";
  if (code === 1 || code === 2) return isDay ? "partlyCloudyDay" : "cloudy";
  if (code === 3) return "cloudy";
  if (code === 45 || code === 48) return "fog";
  if (code >= 51 && code <= 57) return "drizzle";
  if (code >= 61 && code <= 67) return "rain";
  if (code >= 71 && code <= 77) return "snow";
  if (code >= 80 && code <= 82) return "rain";
  if (code === 85 || code === 86) return "snow";
  if (code >= 95) return "thunderstorm";
  return "cloudy";
}

export function WeatherWidget({
  airportCode,
  city,
}: {
  airportCode: string;
  city: string;
}) {
  // Tagging the result with the city it belongs to lets "loading" be derived
  // rather than assigned: anything that doesn't match the current prop is, by
  // definition, still in flight. That also discards responses that arrive
  // after the city has already changed.
  const [result, setResult] = useState<WeatherResult | null>(null);

  useEffect(() => {
    let cancelled = false;

    getCityWeather(city)
      .then((weather) => {
        if (!cancelled) setResult({ city, weather });
      })
      .catch(() => {
        if (!cancelled) setResult({ city, weather: null });
      });

    return () => {
      cancelled = true;
    };
  }, [city]);

  const current = result?.city === city ? result : null;
  const status: Status = !current
    ? "loading"
    : current.weather
      ? "success"
      : "error";
  const weather = current?.weather ?? null;

  const iconKey: WeatherIconKey =
    status === "error"
      ? "unavailable"
      : weather
        ? weatherIconKey(weather.weatherCode, weather.isDay)
        : "cloudy";
  const Icon = WEATHER_ICONS[iconKey];

  return (
    <div className="flex h-full flex-col rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none">
      <div>
        <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
          Arrival weather
        </span>
        <p className="mt-1 font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
          {airportCode}
        </p>
      </div>

      {/* Fills the gap the card's fixed height leaves between header and readout. */}
      <div className="flex flex-1 items-center justify-center py-4">
        <motion.div
          key={status === "success" ? `icon-${weather?.weatherCode}` : status}
          initial={{ opacity: 0, scale: 0.85 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ type: "spring", stiffness: 260, damping: 20 }}
          className="relative"
        >
          {status === "success" && (
            <span
              aria-hidden
              className="absolute inset-0 rounded-full bg-accent/15 blur-3xl dark:bg-accent/20"
            />
          )}
          <Icon
            className={`relative h-28 w-28 ${
              status === "success"
                ? "text-accent"
                : "text-zinc-200 dark:text-zinc-700"
            } ${status === "loading" ? "animate-pulse" : ""}`}
            strokeWidth={1}
          />
        </motion.div>
      </div>

      <div>
        {status === "loading" && (
          <div className="flex items-center gap-2 text-sm text-zinc-400 dark:text-zinc-500">
            <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-zinc-300 border-t-accent dark:border-white/20" />
            Fetching conditions…
          </div>
        )}

        {status === "error" && (
          <p className="text-sm text-zinc-400 dark:text-zinc-500">
            Weather unavailable right now.
          </p>
        )}

        {status === "success" && weather && (
          <>
            <p className="text-4xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
              {weather.temperatureCelsius}°
            </p>
            <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
              {weather.conditions} · Wind {weather.windSpeedKph} kph
            </p>
          </>
        )}
      </div>
    </div>
  );
}
