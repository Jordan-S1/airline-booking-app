export type Gender = "MALE" | "FEMALE" | "OTHER";

export type PaymentMethod =
  | "CREDIT_CARD"
  | "DEBIT_CARD"
  | "PAYPAL"
  | "BANK_TRANSFER";

/** Mirrors com.airlinebookingsystem.dto.passenger.PassengerRequest. */
export interface PassengerRequestDto {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: Gender;
  passportNumber: string;
  nationality: string;
}

/** Mirrors com.airlinebookingsystem.dto.booking.BookingRequest. */
export interface BookingRequestDto {
  flightId: number;
  seatClass: string;
  passengers: PassengerRequestDto[];
}

/** Mirrors com.airlinebookingsystem.dto.booking.BookingResponse. */
export interface BookingResponseDto {
  id: number;
  bookingReference: string;
  flightNumber: string;
  departureAirport: string;
  arrivalAirport: string;
  departureTime: string;
  arrivalTime: string;
  numberOfPassengers: number;
  totalAmount: number;
  status: string;
  seatClass: string;
  userEmail: string;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors com.airlinebookingsystem.dto.payment.PaymentRequest. */
export interface PaymentRequestDto {
  bookingId: number;
  paymentMethod: PaymentMethod;
}

/** Mirrors com.airlinebookingsystem.dto.payment.PaymentResponse. */
export interface PaymentResponseDto {
  id: number;
  transactionId: string;
  bookingId: number;
  bookingReference: string;
  amount: number;
  paymentMethod: PaymentMethod;
  status: "PENDING" | "SUCCESS" | "FAILED" | "REFUNDED";
  paymentGatewayResponse: string | null;
  createdAt: string;
  updatedAt: string;
}
