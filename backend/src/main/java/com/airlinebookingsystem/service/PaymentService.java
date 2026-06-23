package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.payment.PaymentRequest;
import com.airlinebookingsystem.dto.payment.PaymentResponse;
import com.airlinebookingsystem.entity.Booking;
import com.airlinebookingsystem.entity.Payment;
import com.airlinebookingsystem.repository.PaymentRepository;
import com.airlinebookingsystem.repository.BookingRepository;
import com.airlinebookingsystem.exception.BookingException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for handling payment-related operations.
 * Provides methods for creating, processing, and managing payments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final BookingService bookingService;

    /**
     * Creates a new payment record for a booking.
     *
     * @param paymentRequest the payment request details
     * @return PaymentResponse containing payment result
     */
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment for booking ID: {}", paymentRequest.bookingId());

        // Validate booking exists and is in valid state
        Booking booking = validateBookingForPayment(Objects.requireNonNull(paymentRequest.bookingId()));

        // Check if payment already exists for this booking
        Optional<Payment> existingPayment = paymentRepository.findByBookingId(booking.getId());
        if (existingPayment.isPresent() &&
                existingPayment.get().getStatus() == Payment.PaymentStatus.SUCCESS) {
            throw new BookingException("Payment already completed for this booking");
        }

        // Generate unique transaction ID
        String transactionId = generateTransactionId();

        // Create payment entity
        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .booking(booking)
                .amount(paymentRequest.amount())
                .paymentMethod(paymentRequest.paymentMethod())
                .status(Payment.PaymentStatus.PENDING)
                .build();

        // Save payment with pending status
        payment = paymentRepository.save(Objects.requireNonNull(payment));

        try {
            // Process payment through gateway
            String gatewayResponse = paymentGatewayService.processPayment(
                    transactionId,
                    paymentRequest.amount(),
                    paymentRequest.paymentMethod(),
                    paymentRequest.paymentDetails());

            // Update payment status on success
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setPaymentGatewayResponse(gatewayResponse);
            payment = paymentRepository.save(payment);

            // Update booking status to confirmed
            bookingService.confirmBooking(booking.getBookingReference());

            log.info("Payment successful for transaction ID: {}", transactionId);

        } catch (Exception e) {
            // Update payment status on failure
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setPaymentGatewayResponse(e.getMessage());
            paymentRepository.save(payment);

            log.error("Payment failed for transaction ID: {}", transactionId, e);
            throw new BookingException("Payment processing failed: " + e.getMessage());
        }

        return convertToResponseDTO(payment);
    }

    /**
     * Initiates a refund for a payment.
     *
     * @param transactionId the transaction ID of the payment to refund
     * @param refundAmount  the amount to refund (null for full refund)
     * @return PaymentResponseDTO containing refund details
     * @throws BookingException if refund processing fails
     */
    public PaymentResponse refundPayment(String transactionId, BigDecimal refundAmount) {
        log.info("Refunding payment with transaction ID: {}", transactionId);

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", transactionId));

        // Validate payment can be refunded
        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new BookingException("Payment cannot be refunded. Current status: " + payment.getStatus());
        }

        // Use original amount if refund amount not specified
        BigDecimal amountToRefund = refundAmount != null ? refundAmount : payment.getAmount();

        // Validate refund amount
        if (amountToRefund.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed original payment amount");
        }

        try {
            // Process refund through gateway
            String refundResponse = paymentGatewayService.processRefund(
                    transactionId,
                    amountToRefund);

            // Update payment status
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment.setPaymentGatewayResponse(refundResponse);
            payment = paymentRepository.save(payment);

            // Update booking status to canceled
            bookingService.cancelBooking(payment.getBooking().getBookingReference());

            log.info("Refund successful for transaction ID: {}", transactionId);

        } catch (Exception e) {
            log.error("Refund failed for transaction ID: {}", transactionId, e);
            throw new BookingException("Refund processing failed: " + e.getMessage());
        }

        return convertToResponseDTO(payment);
    }

    /**
     * Retrieves payment information by transaction ID.
     *
     * @param transactionId the transaction ID to search for
     * @return PaymentResponseDTO containing payment details
     * @throws ResourceNotFoundException if payment is not found
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        log.info("Retrieving payment with transaction ID: {}", transactionId);
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", transactionId));

        return convertToResponseDTO(payment);
    }

    /**
     * Retrieves payment information by booking ID.
     *
     * @param bookingId the booking ID to search for
     * @return PaymentResponseDTO containing payment details
     * @throws ResourceNotFoundException if payment is not found
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        log.info("Retrieving payment for booking ID: {}", bookingId);

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", bookingId));

        return convertToResponseDTO(payment);
    }

    /**
     * Retrieves all payments with a specific status.
     *
     * @param status the payment status to filter by
     * @return List of PaymentResponse
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status) {
        log.info("Retrieving payments with status: {}", status);

        List<Payment> payments = paymentRepository.findByStatus(status);
        return payments.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves payments within a specific date range.
     *
     * @param startDate the start date and time
     * @param endDate   the end date and time
     * @return List of PaymentResponse
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Retrieving payments between {} and {}", startDate, endDate);

        List<Payment> payments = paymentRepository.findPaymentsByDateRange(startDate, endDate);
        return payments.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Validates if a booking exists and is in a valid state for payment.
     *
     * @param bookingId the booking ID to validate
     * @return the validated Booking entity
     */
    private Booking validateBookingForPayment(@NonNull Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new BookingException("Booking must be in PENDING status for payment");
        }

        return booking;
    }

    /**
     * Retrieves all payments in the system.
     *
     * @return a list of all payments
     */
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        log.debug("Retrieving all payments");
        return paymentRepository.findAll();
    }

    /**
     * Deletes a payment by its ID.
     *
     * @param paymentId the ID of the payment to delete
     * @throws ResourceNotFoundException if payment not found
     */
    public void deletePayment(@NonNull Long paymentId) {
        log.info("Deleting payment with ID: {}", paymentId);

        if (!paymentRepository.existsById(paymentId)) {
            throw new ResourceNotFoundException("Payment", paymentId);
        }

        paymentRepository.deleteById(paymentId);
        log.info("Payment deleted successfully with ID: {}", paymentId);
    }

    /**
     * Generates a unique transaction ID for a payment.
     *
     * @return a unique transaction ID
     */
    private String generateTransactionId() {
        String transactionId;
        do {
            transactionId = "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        } while (paymentRepository.existsByTransactionId(transactionId));

        return transactionId;
    }

    /**
     * Converts Payment entity to PaymentResponse.
     *
     * @param payment the Payment entity to convert
     * @return PaymentResponse
     */
    private PaymentResponse convertToResponseDTO(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getTransactionId(),
                payment.getBooking().getId(),
                payment.getBooking().getBookingReference(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getPaymentGatewayResponse(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
