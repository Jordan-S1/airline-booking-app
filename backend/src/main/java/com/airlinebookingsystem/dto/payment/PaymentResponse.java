package com.airlinebookingsystem.dto.payment;

import com.airlinebookingsystem.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String transactionId,
        Long bookingId,
        String bookingReference,
        BigDecimal amount,
        Payment.PaymentMethod paymentMethod,
        Payment.PaymentStatus status,
        String paymentGatewayResponse,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
