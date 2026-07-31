import { apiClient } from "../lib/apiClient";
import type { PaymentRequestDto, PaymentResponseDto } from "../types/booking";

export async function createPayment(
  request: PaymentRequestDto,
): Promise<PaymentResponseDto> {
  const { data } = await apiClient.post<PaymentResponseDto>(
    "/payments",
    request,
  );
  return data;
}

/**
 * Refunds a charge in full. The server also cancels the booking and returns its
 * seats, so this is the single compensating action for an already-paid leg —
 * cancelling separately would leave the charge standing.
 */
export async function refundPayment(
  transactionId: string,
): Promise<PaymentResponseDto> {
  const { data } = await apiClient.post<PaymentResponseDto>(
    `/payments/${transactionId}/refund`,
  );
  return data;
}
