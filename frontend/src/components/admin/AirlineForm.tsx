import { useState } from "react";
import axios from "axios";
import { AdminFormModal, Field } from "./AdminFormModal";
import { createAirline, updateAirline } from "../../api/airlines";
import type { AirlineRequestDto, AirlineResponseDto } from "../../types/airline";

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

export function AirlineForm({
  airline,
  onSaved,
  onClose,
}: {
  /** Omitted when creating. */
  airline?: AirlineResponseDto;
  onSaved: (saved: AirlineResponseDto) => void;
  onClose: () => void;
}) {
  const isEdit = Boolean(airline);
  const [code, setCode] = useState(airline?.code ?? "");
  const [name, setName] = useState(airline?.name ?? "");
  const [country, setCountry] = useState(airline?.country ?? "");
  const [website, setWebsite] = useState(airline?.website ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async () => {
    setError(null);
    setIsSubmitting(true);

    const request: AirlineRequestDto = {
      code: code.trim().toUpperCase(),
      name: name.trim(),
      country: country.trim(),
      website: website.trim() || null,
      logoUrl: airline?.logoUrl ?? null,
      // Editing must not silently reactivate a carrier taken out of service.
      active: airline?.active ?? true,
    };

    try {
      const saved = airline
        ? await updateAirline(airline.id, request)
        : await createAirline(request);
      onSaved(saved);
      onClose();
    } catch (err) {
      setError(extractError(err, "Could not save the airline."));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AdminFormModal
      title={isEdit ? `Edit ${airline?.code}` : "New airline"}
      subtitle={
        isEdit
          ? "The IATA code is a permanent identifier and cannot be changed."
          : "Two-letter IATA code, e.g. EI for Aer Lingus."
      }
      error={error}
      submitLabel={isEdit ? "Save changes" : "Create airline"}
      isSubmitting={isSubmitting}
      onSubmit={submit}
      onClose={onClose}
    >
      <Field
        label="IATA code"
        value={code}
        onChange={(v) => setCode(v.toUpperCase().slice(0, 2))}
        placeholder="EI"
        disabled={isEdit}
        hint={isEdit ? "(fixed)" : "2 characters"}
      />
      <Field label="Name" value={name} onChange={setName} placeholder="Aer Lingus" />
      <Field label="Country" value={country} onChange={setCountry} placeholder="Ireland" />
      <Field
        label="Website"
        value={website}
        onChange={setWebsite}
        placeholder="https://www.aerlingus.com"
        hint="optional"
      />
    </AdminFormModal>
  );
}
