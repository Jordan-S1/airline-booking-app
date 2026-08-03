import { useState } from "react";
import axios from "axios";
import { AdminFormModal, Field } from "./AdminFormModal";
import { createAirport, updateAirport } from "../../api/airports";
import type { AirportRequestDto } from "../../types/airport";
import type { AirportResponseDto } from "../../types/flight";

function extractError(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data;
    const validation = data?.validationErrors as Record<string, string> | undefined;
    if (validation && Object.keys(validation).length > 0) {
      return Object.values(validation).join(" ");
    }
    return data?.message ?? data?.error ?? fallback;
  }
  return fallback;
}

/** Empty string means "not provided"; anything unparseable is rejected below. */
function toNumberOrNull(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) return null;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : NaN;
}

export function AirportForm({
  airport,
  onSaved,
  onClose,
}: {
  airport?: AirportResponseDto;
  onSaved: (saved: AirportResponseDto) => void;
  onClose: () => void;
}) {
  const isEdit = Boolean(airport);
  const [code, setCode] = useState(airport?.code ?? "");
  const [name, setName] = useState(airport?.name ?? "");
  const [city, setCity] = useState(airport?.city ?? "");
  const [country, setCountry] = useState(airport?.country ?? "");
  const [countryCode, setCountryCode] = useState(airport?.countryCode ?? "");
  const [timezone, setTimezone] = useState(airport?.timezone ?? "");
  const [latitude, setLatitude] = useState(
    airport?.latitude != null ? String(airport.latitude) : "",
  );
  const [longitude, setLongitude] = useState(
    airport?.longitude != null ? String(airport.longitude) : "",
  );
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async () => {
    setError(null);

    const lat = toNumberOrNull(latitude);
    const lon = toNumberOrNull(longitude);
    if (Number.isNaN(lat) || Number.isNaN(lon)) {
      setError("Latitude and longitude must be numbers, or left blank.");
      return;
    }

    // A timezone that Intl cannot resolve would break every time this airport's
    // flights are rendered, so it is worth refusing here rather than later.
    if (timezone.trim()) {
      try {
        new Intl.DateTimeFormat("en-US", { timeZone: timezone.trim() });
      } catch {
        setError(`"${timezone.trim()}" is not a recognised IANA timezone.`);
        return;
      }
    }

    setIsSubmitting(true);
    const request: AirportRequestDto = {
      code: code.trim().toUpperCase(),
      name: name.trim(),
      city: city.trim(),
      country: country.trim(),
      countryCode: countryCode.trim().toUpperCase() || null,
      timezone: timezone.trim() || null,
      latitude: lat,
      longitude: lon,
    };

    try {
      const saved = airport
        ? await updateAirport(airport.id, request)
        : await createAirport(request);
      onSaved(saved);
      onClose();
    } catch (err) {
      setError(extractError(err, "Could not save the airport."));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AdminFormModal
      title={isEdit ? `Edit ${airport?.code}` : "New airport"}
      subtitle="Coordinates and timezone drive the route map and local departure times — an airport without them still books, but renders poorly."
      error={error}
      submitLabel={isEdit ? "Save changes" : "Create airport"}
      isSubmitting={isSubmitting}
      onSubmit={submit}
      onClose={onClose}
    >
      <div className="grid grid-cols-2 gap-3">
        <Field
          label="IATA code"
          value={code}
          onChange={(v) => setCode(v.toUpperCase().slice(0, 3))}
          placeholder="DUB"
          disabled={isEdit}
          hint={isEdit ? "(fixed)" : undefined}
        />
        <Field
          label="Country code"
          value={countryCode}
          onChange={(v) => setCountryCode(v.toUpperCase().slice(0, 2))}
          placeholder="IE"
          hint="ISO alpha-2"
        />
      </div>
      <Field
        label="Name"
        value={name}
        onChange={setName}
        placeholder="Dublin Airport"
      />
      <div className="grid grid-cols-2 gap-3">
        <Field label="City" value={city} onChange={setCity} placeholder="Dublin" />
        <Field
          label="Country"
          value={country}
          onChange={setCountry}
          placeholder="Ireland"
        />
      </div>
      <Field
        label="Timezone"
        value={timezone}
        onChange={setTimezone}
        placeholder="Europe/Dublin"
        hint="IANA"
      />
      <div className="grid grid-cols-2 gap-3">
        <Field
          label="Latitude"
          value={latitude}
          onChange={setLatitude}
          placeholder="53.421333"
        />
        <Field
          label="Longitude"
          value={longitude}
          onChange={setLongitude}
          placeholder="-6.270075"
        />
      </div>
    </AdminFormModal>
  );
}
