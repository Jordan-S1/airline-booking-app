import { useEffect, useState } from "react";
import {
  Cloud,
  CloudDrizzle,
  CloudFog,
  CloudLightning,
  CloudRain,
  CloudSnow,
  CloudSun,
  Moon,
  Sun,
  type LucideIcon,
} from "lucide-react";
import { getCityWeather, type CityWeather } from "../api/weather";

type Status = "loading" | "error" | "success";

/**
 * Picks an icon for a WMO interpretation code. Clear/partly-cloudy states swap
 * to a night variant after dark; precipitation looks the same either way.
 */
function weatherIcon(code: number, isDay: boolean): LucideIcon {
  if (code === 0) return isDay ? Sun : Moon;
  if (code === 1 || code === 2) return isDay ? CloudSun : Cloud;
  if (code === 3) return Cloud;
  if (code === 45 || code === 48) return CloudFog;
  if (code >= 51 && code <= 57) return CloudDrizzle;
  if (code >= 61 && code <= 67) return CloudRain;
  if (code >= 71 && code <= 77) return CloudSnow;
  if (code >= 80 && code <= 82) return CloudRain;
  if (code === 85 || code === 86) return CloudSnow;
  if (code >= 95) return CloudLightning;
  return Cloud;
}

export function WeatherWidget({
  airportCode,
  city,
}: {
  airportCode: string;
  city: string;
}) {
  const [status, setStatus] = useState<Status>("loading");
  const [weather, setWeather] = useState<CityWeather | null>(null);

  useEffect(() => {
    let cancelled = false;
    setStatus("loading");

    getCityWeather(city)
      .then((result) => {
        if (!cancelled) {
          setWeather(result);
          setStatus("success");
        }
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });

    return () => {
      cancelled = true;
    };
  }, [city]);

  const Icon = weather ? weatherIcon(weather.weatherCode, weather.isDay) : Sun;

  return (
    <div className="flex h-full flex-col justify-between rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-obsidian-raised dark:shadow-none">
      <div className="flex items-start justify-between">
        <div>
          <span className="text-[11px] font-medium uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
            Arrival weather
          </span>
          <p className="mt-1 font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
            {airportCode}
          </p>
        </div>
        <Icon
          className={`h-6 w-6 ${
            status === "success" ? "text-accent" : "text-zinc-300 dark:text-zinc-600"
          }`}
          strokeWidth={1.8}
        />
      </div>

      <div className="mt-6">
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
