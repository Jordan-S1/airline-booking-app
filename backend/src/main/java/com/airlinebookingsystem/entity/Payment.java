package com.airlinebookingsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a payment entity in the airline booking system.
 * This entity stores payment information, including transaction details,
 * booking reference, amount, payment method, and status.
 *
 * @since 1.0
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "booking")
public class Payment {

    /**
     * Unique identifier for the payment record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique transaction identifier for the payment.
     */
    @Column(unique = true, nullable = false)
    private String transactionId;

    /**
     * Associated booking for which the payment is made.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * Payment amount with precision of 10 digits and 2 decimal places.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Method used for the payment (e.g., credit card, PayPal).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    /**
     * Current status of the payment. Defaults to PENDING.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Response received from the payment gateway.
     */
    private String paymentGatewayResponse;

    /**
     * Timestamp when the payment record was created.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp when the payment record was last updated.
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Enumeration of supported payment methods in the system.
     */
    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER
    }

    /**
     * Enumeration of possible payment statuses.
     * PENDING: Initial state when payment is created
     * SUCCESS: Payment has been successfully processed
     * FAILED: Payment processing failed
     * REFUNDED: Payment has been refunded to the customer
     */
    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, REFUNDED
    }
}
