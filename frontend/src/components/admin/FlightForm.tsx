import { useState } from "react";
import axios from "axios";
import { AdminFormModal, Field } from "./AdminFormModal";
import { SelectField, type SelectOption } from "../SelectField";
import { createFlight, updateFlight } from "../../api/flights";
import { localInputToUtc, utcToLocalInput } from "../../lib/datetime";
import type { AirlineResponseDto } from "../../types/airline";
import type {
  AirportResponseDto,
  FlightRequestDto,
  FlightResponseDto,
  FlightStatus,
} from "../../types/flight";

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

const STATUS_OPTIONS: SelectOption<FlightStatus>[] = [
  { value: "SCHEDULED", label: "Scheduled" },
  { value: "DELAYED", label: "Delayed" },
  { value: "CANCELLED", label: "Cancelled" },
];

export function FlightForm({
  flight,
  airlines,
  airports,
  onSaved,
  onClose,
}: {
  flight?: FlightResponseDto;
  airlines: AirlineResponseDto[];
  airports: AirportResponseDto[];
  onSaved: (saved: FlightResponseDto) => void;
  onClose: () => void;
}) {
  const isEdit = Boolean(flight);

  const airlineOptions: SelectOption<string>[] = airlines
    .filter((a) => a.active || a.code === flight?.airlineCode)
    .map((a) => ({ value: a.code, label: `${a.code} — ${a.name}` }));
  const airportOptions: SelectOption<string>[] = airports.map((a) => ({
    value: a.code,
    label: `${a.code} — ${a.city}`,
  }));

  const [flightNumber, setFlightNumber] = useState(flight?.flightNumber ?? "");
  const [airlineCode, setAirlineCode] = useState(
    flight?.airlineCode ?? airlineOptions[0]?.value ?? "",
  );
  const [departureCode, setDepartureCode] = useState(
    flight?.departureAirportCode ?? airportOptions[0]?.value ?? "",
  );
  const [arrivalCode, setArrivalCode] = useState(
    flight?.arrivalAirportCode ?? airportOptions[1]?.value ?? "",
  );

  const zoneOf = (code: string) =>
    airports.find((a) => a.code === code)?.timezone ?? null;

  // Times are entered as local at each end — which is how a timetable is read —
  // and converted to the UTC instants the API stores on submit.
  const [departureLocal, setDepartureLocal] = useState(
    flight ? utcToLocalInput(flight.departureTime, zoneOf(flight.departureAirportCode)) : "",
  );
  const [arrivalLocal, setArrivalLocal] = useState(
    flight ? utcToLocalInput(flight.arrivalTime, zoneOf(flight.arrivalAirportCode)) : "",
  );

  const [economySeats, setEconomySeats] = useState(String(flight?.economySeats ?? 150));
  const [businessSeats, setBusinessSeats] = useState(String(flight?.businessSeats ?? 30));
  const [firstSeats, setFirstSeats] = useState(String(flight?.firstClassSeats ?? 0));
  const [economyPrice, setEconomyPrice] = useState(String(flight?.economyPrice ?? 99.99));
  const [businessPrice, setBusinessPrice] = useState(String(flight?.businessPrice ?? 329.99));
  const [firstPrice, setFirstPrice] = useState(String(flight?.firstClassPrice ?? 0));
  const [aircraft, setAircraft] = useState(flight?.aircraft ?? "");
  const [flightStatus, setFlightStatus] = useState<FlightStatus>(
    flight?.status ?? "SCHEDULED",
  );

  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const departureUtc = departureLocal
    ? localInputToUtc(departureLocal, zoneOf(departureCode))
    : "";
  const arrivalUtc = arrivalLocal
    ? localInputToUtc(arrivalLocal, zoneOf(arrivalCode))
    : "";

  // Shown live so the derived duration is visible before saving — the service
  // computes it from these two instants, and a negative one is a sure sign the
  // local times were entered against the wrong ends.
  const durationMinutes =
    departureUtc && arrivalUtc
      ? Math.round(
          (new Date(`${arrivalUtc}Z`).getTime() -
            new Date(`${departureUtc}Z`).getTime()) /
            60000,
        )
      : null;

  const submit = async () => {
    setError(null);

    if (departureCode === arrivalCode) {
      setError("Departure and arrival airports must differ.");
      return;
    }
    if (!departureLocal || !arrivalLocal) {
      setError("Both departure and arrival times are required.");
      return;
    }
    if (durationMinutes !== null && durationMinutes <= 0) {
      setError("Arrival must be after departure once both are read in their own timezone.");
      return;
    }

    setIsSubmitting(true);
    const request: FlightRequestDto = {
      flightNumber: flightNumber.trim().toUpperCase(),
      airlineCode,
      departureAirportCode: departureCode,
      arrivalAirportCode: arrivalCode,
      departureTime: departureUtc,
      arrivalTime: arrivalUtc,
      economySeats: Number(economySeats) || 0,
      businessSeats: Number(businessSeats) || 0,
      firstClassSeats: Number(firstSeats) || 0,
      economyPrice: Number(economyPrice) || 0,
      businessPrice: Number(businessPrice) || 0,
      firstClassPrice: Number(firstPrice) || 0,
      aircraft: aircraft.trim() || null,
      status: flightStatus,
    };

    try {
      const saved = flight
        ? await updateFlight(flight.id, request)
        : await createFlight(request);
      onSaved(saved);
      onClose();
    } catch (err) {
      setError(extractError(err, "Could not save the flight."));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AdminFormModal
      title={isEdit ? `Edit ${flight?.flightNumber}` : "New flight"}
      subtitle="Times are entered in each airport's own local time and stored as UTC."
      error={error}
      submitLabel={isEdit ? "Save changes" : "Create flight"}
      isSubmitting={isSubmitting}
      onSubmit={submit}
      onClose={onClose}
    >
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Field
          label="Flight number"
          value={flightNumber}
          onChange={(v) => setFlightNumber(v.toUpperCase())}
          placeholder="EI156"
        />
        <SelectField
          label="Airline"
          value={airlineCode}
          options={airlineOptions}
          onChange={setAirlineCode}
        />
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <SelectField
          label="From"
          value={departureCode}
          options={airportOptions}
          onChange={setDepartureCode}
        />
        <SelectField
          label="To"
          value={arrivalCode}
          options={airportOptions}
          onChange={setArrivalCode}
        />
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Field
          label="Departs"
          type="datetime-local"
          value={departureLocal}
          onChange={setDepartureLocal}
          hint={zoneOf(departureCode) ?? "local"}
        />
        <Field
          label="Arrives"
          type="datetime-local"
          value={arrivalLocal}
          onChange={setArrivalLocal}
          hint={zoneOf(arrivalCode) ?? "local"}
        />
      </div>

      {durationMinutes !== null && (
        <p
          className={`px-1 text-xs ${
            durationMinutes <= 0
              ? "text-red-600 dark:text-red-400"
              : "text-zinc-400 dark:text-zinc-500"
          }`}
        >
          {durationMinutes > 0
            ? `Block time ${Math.floor(durationMinutes / 60)}h ${durationMinutes % 60}m`
            : "Arrival is before departure"}
        </p>
      )}

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Field label="Economy seats" value={economySeats} onChange={setEconomySeats} />
        <Field label="Business seats" value={businessSeats} onChange={setBusinessSeats} />
        <Field label="First seats" value={firstSeats} onChange={setFirstSeats} />
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Field label="Economy €" value={economyPrice} onChange={setEconomyPrice} />
        <Field label="Business €" value={businessPrice} onChange={setBusinessPrice} />
        <Field label="First €" value={firstPrice} onChange={setFirstPrice} />
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Field
          label="Aircraft"
          value={aircraft}
          onChange={setAircraft}
          placeholder="Airbus A320"
          hint="optional"
        />
        <SelectField
          label="Status"
          value={flightStatus}
          options={STATUS_OPTIONS}
          onChange={setFlightStatus}
        />
      </div>
    </AdminFormModal>
  );
}
