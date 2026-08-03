import { apiClient } from "../lib/apiClient";
import type { BookingRequestDto, BookingResponseDto } from "../types/booking";

export async function createBooking(
  userId: number,
  request: BookingRequestDto,
): Promise<BookingResponseDto> {
  const { data } = await apiClient.post<BookingResponseDto>(
    `/bookings/user/${userId}`,
    request,
  );
  return data;
}

export async function confirmBooking(
  bookingReference: string,
): Promise<BookingResponseDto> {
  const { data } = await apiClient.patch<BookingResponseDto>(
    `/bookings/${bookingReference}/confirm`,
  );
  return data;
}

export async function getBookingsByUser(
  userId: number,
): Promise<BookingResponseDto[]> {
  const { data } = await apiClient.get<BookingResponseDto[]>(
    `/bookings/user/${userId}`,
  );
  return data;
}

/**
 * Admin only — every booking in the system with the given status, across all
 * customers. Non-admin tokens are rejected server-side with 403.
 */
export async function getBookingsByStatus(
  status: string,
): Promise<BookingResponseDto[]> {
  const { data } = await apiClient.get<BookingResponseDto[]>(
    `/bookings/status/${status}`,
  );
  return data;
}

export async function getBooking(
  bookingReference: string,
): Promise<BookingResponseDto> {
  const { data } = await apiClient.get<BookingResponseDto>(
    `/bookings/${bookingReference}`,
  );
  return data;
}

export async function cancelBooking(
  bookingReference: string,
): Promise<BookingResponseDto> {
  const { data } = await apiClient.patch<BookingResponseDto>(
    `/bookings/${bookingReference}/cancel`,
  );
  return data;
}
