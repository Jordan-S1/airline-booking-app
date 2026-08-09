package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.payment.PaymentRequest;
import com.airlinebookingsystem.dto.payment.PaymentResponse;
import com.airlinebookingsystem.entity.Booking;
import com.airlinebookingsystem.entity.Payment;
import com.airlinebookingsystem.entity.User;
import com.airlinebookingsystem.exception.BookingException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.BookingRepository;
import com.airlinebookingsystem.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaymentService.
 *
 * <p>The rules worth pinning down here are the ones about money: that the
 * amount charged comes from the booking and never from the request, that a
 * booking cannot be paid for twice, that a failed charge leaves a record
 * rather than vanishing, and that a refund goes through the one path that
 * also releases the seat. All collaborators are mocked, so these hold
 * regardless of what the gateway does.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentGatewayService paymentGatewayService;
    @Mock private BookingService bookingService;

    @InjectMocks private PaymentService paymentService;

    private static final String REF = "BK17832167460316843";
    private static final String TXN = "TXN_A1B2C3D4E5F6A7B8";
    private static final BigDecimal BOOKING_TOTAL = new BigDecimal("179.98");

    private Booking booking;

    @BeforeEach
    void setUp() throws Exception {
        User customer = User.builder()
                .id(1L)
                .email("jordan@example.com")
                .firstName("Jordan").lastName("Test")
                .role(User.Role.CUSTOMER)
                .build();

        booking = Booking.builder()
                .id(1L)
                .bookingReference(REF)
                .user(customer)
                .totalAmount(BOOKING_TOTAL)
                .status(Booking.BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.existsByTransactionId(anyString())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGatewayService.processPayment(anyString(), any(), any(), any()))
                .thenReturn("GATEWAY_OK");
    }

    private PaymentRequest request() {
        return new PaymentRequest(1L, Payment.PaymentMethod.CREDIT_CARD);
    }

    private Payment paymentWith(Payment.PaymentStatus status) {
        return Payment.builder()
                .id(1L)
                .transactionId(TXN)
                .booking(booking)
                .amount(BOOKING_TOTAL)
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("charges the booking's total — the request cannot name an amount")
        void chargesTheBookingTotal() throws Exception {
            paymentService.processPayment(request());

            ArgumentCaptor<BigDecimal> charged = ArgumentCaptor.forClass(BigDecimal.class);
            verify(paymentGatewayService)
                    .processPayment(anyString(), charged.capture(), any(), any());

            assertThat(charged.getValue()).isEqualByComparingTo(BOOKING_TOTAL);
        }

        @Test
        @DisplayName("records the same amount it charged")
        void recordsTheAmountCharged() {
            PaymentResponse response = paymentService.processPayment(request());

            assertThat(response.amount()).isEqualByComparingTo(BOOKING_TOTAL);
            assertThat(response.status()).isEqualTo(Payment.PaymentStatus.SUCCESS);
            assertThat(response.bookingReference()).isEqualTo(REF);
        }

        @Test
        @DisplayName("confirms the booking once the charge succeeds")
        void confirmsBookingOnSuccess() {
            paymentService.processPayment(request());
            verify(bookingService).confirmBooking(REF);
        }

        @Test
        @DisplayName("writes the payment before charging, so a crash mid-call leaves a trace")
        void persistsBeforeCharging() throws Exception {
            paymentService.processPayment(request());

            InOrder order = inOrder(paymentRepository, paymentGatewayService);
            order.verify(paymentRepository).save(any(Payment.class));
            order.verify(paymentGatewayService).processPayment(anyString(), any(), any(), any());
        }

        @Test
        @DisplayName("a gateway failure is recorded as FAILED, not left PENDING")
        void recordsFailureWhenGatewayRejects() throws Exception {
            when(paymentGatewayService.processPayment(anyString(), any(), any(), any()))
                    .thenThrow(new RuntimeException("card declined"));

            assertThatThrownBy(() -> paymentService.processPayment(request()))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("card declined");

            ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository, times(2)).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("a failed charge never confirms the booking")
        void doesNotConfirmBookingOnFailure() throws Exception {
            when(paymentGatewayService.processPayment(anyString(), any(), any(), any()))
                    .thenThrow(new RuntimeException("card declined"));

            assertThatThrownBy(() -> paymentService.processPayment(request()))
                    .isInstanceOf(BookingException.class);

            verify(bookingService, never()).confirmBooking(anyString());
        }

        @Test
        @DisplayName("refuses to charge a booking that is already paid")
        void refusesDoublePayment() throws Exception {
            when(paymentRepository.findByBookingId(1L))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.SUCCESS)));

            assertThatThrownBy(() -> paymentService.processPayment(request()))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("already completed");

            verify(paymentGatewayService, never())
                    .processPayment(anyString(), any(), any(), any());
        }

        @Test
        @DisplayName("allows another attempt after a failed one")
        void allowsRetryAfterFailure() throws Exception {
            when(paymentRepository.findByBookingId(1L))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.FAILED)));

            PaymentResponse response = paymentService.processPayment(request());

            assertThat(response.status()).isEqualTo(Payment.PaymentStatus.SUCCESS);
            verify(paymentGatewayService).processPayment(anyString(), any(), any(), any());
        }

        @ParameterizedTest
        @EnumSource(value = Booking.BookingStatus.class,
                names = { "CONFIRMED", "CANCELLED", "COMPLETED" })
        @DisplayName("refuses any booking that is not PENDING")
        void refusesNonPendingBooking(Booking.BookingStatus status) throws Exception {
            booking.setStatus(status);

            assertThatThrownBy(() -> paymentService.processPayment(request()))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("PENDING");

            verify(paymentGatewayService, never())
                    .processPayment(anyString(), any(), any(), any());
        }

        @Test
        @DisplayName("throws when the booking does not exist")
        void throwsForUnknownBooking() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processPayment(request()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("generates a fresh transaction id, retrying on collision")
        void retriesOnTransactionIdCollision() {
            when(paymentRepository.existsByTransactionId(anyString()))
                    .thenReturn(true, false);

            paymentService.processPayment(request());

            verify(paymentRepository, times(2)).existsByTransactionId(anyString());
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        @DisplayName("refunds by cancelling the booking, so the seat is released too")
        void refundGoesThroughCancellation() {
            when(paymentRepository.findByTransactionId(TXN))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.SUCCESS)))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.REFUNDED)));

            paymentService.refundPayment(TXN, null);

            verify(bookingService).cancelBooking(REF);
        }

        @Test
        @DisplayName("returns the settled REFUNDED record, not the SUCCESS it read first")
        void returnsSettledStatus() {
            when(paymentRepository.findByTransactionId(TXN))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.SUCCESS)))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.REFUNDED)));

            PaymentResponse response = paymentService.refundPayment(TXN, null);

            assertThat(response.status()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        }

        @ParameterizedTest
        @EnumSource(value = Payment.PaymentStatus.class,
                names = { "PENDING", "FAILED", "REFUNDED" })
        @DisplayName("refuses to refund anything that was not a successful charge")
        void refusesNonSuccessfulPayment(Payment.PaymentStatus status) {
            when(paymentRepository.findByTransactionId(TXN))
                    .thenReturn(Optional.of(paymentWith(status)));

            assertThatThrownBy(() -> paymentService.refundPayment(TXN, null))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("cannot be refunded");

            verify(bookingService, never()).cancelBooking(anyString());
        }

        @Test
        @DisplayName("throws when the transaction is unknown")
        void throwsForUnknownTransaction() {
            when(paymentRepository.findByTransactionId(TXN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.refundPayment(TXN, null))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(bookingService, never()).cancelBooking(anyString());
        }

        @Test
        @DisplayName("a partial amount is ignored — refunds are always the full charge")
        void partialAmountIsIgnored() {
            when(paymentRepository.findByTransactionId(TXN))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.SUCCESS)))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.REFUNDED)));

            PaymentResponse response = paymentService.refundPayment(TXN, new BigDecimal("10.00"));

            // The booking is cancelled outright rather than partly unwound.
            verify(bookingService).cancelBooking(REF);
            assertThat(response.amount()).isEqualByComparingTo(BOOKING_TOTAL);
        }
    }

    @Nested
    @DisplayName("lookups")
    class Lookups {

        @Test
        @DisplayName("finds a payment by transaction id")
        void findsByTransactionId() {
            when(paymentRepository.findByTransactionId(TXN))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.SUCCESS)));

            assertThat(paymentService.getPaymentByTransactionId(TXN).transactionId())
                    .isEqualTo(TXN);
        }

        @Test
        @DisplayName("throws for an unknown transaction id")
        void throwsForUnknownTransactionId() {
            when(paymentRepository.findByTransactionId(TXN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByTransactionId(TXN))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("finds a payment by booking id")
        void findsByBookingId() {
            when(paymentRepository.findByBookingId(1L))
                    .thenReturn(Optional.of(paymentWith(Payment.PaymentStatus.SUCCESS)));

            assertThat(paymentService.getPaymentByBookingId(1L).bookingId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws when no payment exists for the booking")
        void throwsWhenBookingHasNoPayment() {
            when(paymentRepository.findByBookingId(eq(99L))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByBookingId(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
