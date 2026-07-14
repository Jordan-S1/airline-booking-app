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
