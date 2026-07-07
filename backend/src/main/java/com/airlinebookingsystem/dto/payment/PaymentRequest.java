package com.airlinebookingsystem.dto.payment;

import com.airlinebookingsystem.entity.Payment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(

        @NotNull(message = "Booking ID is required")
        Long bookingId,

        @NotNull(message = "Payment method is required")
        Payment.PaymentMethod paymentMethod
) {}
