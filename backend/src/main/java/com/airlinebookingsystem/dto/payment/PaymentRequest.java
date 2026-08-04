package com.airlinebookingsystem.dto.payment;

import com.airlinebookingsystem.entity.Payment;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(

        @NotNull(message = "Booking ID is required")
        Long bookingId,

        @NotNull(message = "Payment method is required")
        Payment.PaymentMethod paymentMethod
) {}
