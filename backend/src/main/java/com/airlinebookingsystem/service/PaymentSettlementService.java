package com.airlinebookingsystem.service;

import com.airlinebookingsystem.entity.Booking;
import com.airlinebookingsystem.entity.Payment;
import com.airlinebookingsystem.exception.BookingException;
import com.airlinebookingsystem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Moves money back for a booking, and nothing else.
 *
 * <p>Refunding and cancelling are two halves of one action, but they cannot
 * live in the same class: cancelling a booking has to refund it, and refunding
 * a payment has to cancel its booking, so putting both in either service makes
 * {@code BookingService} and {@code PaymentService} depend on each other in a
 * circle. This holds the half that neither owns — it touches payments only, so
 * both can call it without calling each other.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentSettlementService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;

    /**
     * Returns the money for a booking if it was ever charged.
     *
     * <p>Deliberately does not touch the booking. A booking with no payment, or
     * one whose payment failed or was already refunded, is not an error — there
     * is simply nothing to give back, and an empty result says so.
     *
     * @return the refunded payment, or empty if there was nothing to refund
     * @throws BookingException if the gateway rejects the refund, so the caller's
     *                          transaction rolls back rather than leaving a
     *                          booking cancelled but still charged
     */
    public Optional<Payment> refundIfCharged(Booking booking) {
        Optional<Payment> existing = paymentRepository.findByBookingId(booking.getId());

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Payment payment = existing.get();
        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            log.debug("Booking {} has a {} payment — nothing to refund",
                    booking.getBookingReference(), payment.getStatus());
            return Optional.empty();
        }

        try {
            String gatewayResponse = paymentGatewayService.processRefund(
                    payment.getTransactionId(), payment.getAmount());

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment.setPaymentGatewayResponse(gatewayResponse);

            log.info("Refunded {} ({}) for booking {}",
                    payment.getTransactionId(), payment.getAmount(),
                    booking.getBookingReference());

            return Optional.of(paymentRepository.save(payment));

        } catch (Exception e) {
            log.error("Refund failed for transaction {}", payment.getTransactionId(), e);
            throw new BookingException("Refund processing failed: " + e.getMessage());
        }
    }
}
