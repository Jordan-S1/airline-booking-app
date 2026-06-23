package com.airlinebookingsystem.dto.payment;

import com.airlinebookingsystem.entity.Payment;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentRequest(
        Long bookingId,
        BigDecimal amount,
        Payment.PaymentMethod paymentMethod,
        Map<String, String> paymentDetails
) {}
