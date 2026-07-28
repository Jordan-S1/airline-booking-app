import axios from "axios";

/**
 * Open-Meteo is free, keyless, and CORS-open — no backend involvement needed.
 * Docs: https://open-meteo.com/en/docs
 */
const geocodingClient = axios.create({
  baseURL: "https://geocoding-api.open-meteo.com/v1",
});

const forecastClient = axios.create({
  baseURL: "https://api.open-meteo.com/v1",
});

// WMO weather interpretation codes → short human labels.
const WEATHER_CODE_LABELS: Record<number, string> = {
  0: "Clear",
  1: "Mostly clear",
  2: "Partly cloudy",
  3: "Overcast",
  45: "Fog",
  48: "Fog",
  51: "Light drizzle",
  53: "Drizzle",
  55: "Heavy drizzle",
  61: "Light rain",
  63: "Rain",
  65: "Heavy rain",
  71: "Light snow",
  73: "Snow",
  75: "Heavy snow",
  80: "Rain showers",
  81: "Rain showers",
  82: "Violent showers",
  95: "Thunderstorm",
  96: "Thunderstorm",
  99: "Thunderstorm",
};

export interface CityWeather {
  city: string;
  temperatureCelsius: number;
  windSpeedKph: number;
  conditions: string;
  /** Raw WMO interpretation code — drives which icon the widget shows. */
  weatherCode: number;
  isDay: boolean;
  observedAt: string;
}

interface GeocodingResult {
  results?: { latitude: number; longitude: number; name: string }[];
}

interface ForecastResult {
  current: {
    temperature_2m: number;
    wind_speed_10m: number;
    weather_code: number;
    is_day: number;
    time: string;
  };
}

export async function getCityWeather(city: string): Promise<CityWeather> {
  const { data: geocoding } = await geocodingClient.get<GeocodingResult>(
    "/search",
    { params: { name: city, count: 1 } },
  );

  const location = geocoding.results?.[0];
  if (!location) {
    throw new Error(`Could not find coordinates for "${city}"`);
  }

  const { data: forecast } = await forecastClient.get<ForecastResult>(
    "/forecast",
    {
      params: {
        latitude: location.latitude,
        longitude: location.longitude,
        current: "temperature_2m,wind_speed_10m,weather_code,is_day",
      },
    },
  );

  return {
    city,
    temperatureCelsius: Math.round(forecast.current.temperature_2m),
    windSpeedKph: Math.round(forecast.current.wind_speed_10m),
    conditions: WEATHER_CODE_LABELS[forecast.current.weather_code] ?? "Unknown",
    weatherCode: forecast.current.weather_code,
    isDay: forecast.current.is_day === 1,
    observedAt: forecast.current.time,
  };
}
