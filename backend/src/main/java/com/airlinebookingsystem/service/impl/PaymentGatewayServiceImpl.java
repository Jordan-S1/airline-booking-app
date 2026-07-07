package com.airlinebookingsystem.service.impl;

import com.airlinebookingsystem.entity.Payment;
import com.airlinebookingsystem.service.PaymentGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock implementation of PaymentGatewayService for development/testing.
 * Replace it with actual payment gateway integration in production.
 */
@Service
@Slf4j
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    @Override
    public String processPayment(String transactionId, BigDecimal amount,
                                 Payment.PaymentMethod paymentMethod,
                                 Object paymentDetails) throws Exception {

        log.info("Processing payment - Transaction ID: {}, Amount: {}, Method: {}",
                transactionId, amount, paymentMethod);

        // Simulate payment processing delay
        Thread.sleep(1000);

        // Simulate random payment failures for testing (10% failure rate)
        if (Math.random() < 0.1) {
            throw new Exception("Payment gateway returned error: Insufficient funds");
        }

        // Simulate successful payment
        String gatewayTransactionId = UUID.randomUUID().toString();
        String response = String.format("Payment successful. Gateway Transaction ID: %s", gatewayTransactionId);

        log.info("Payment processed successfully: {}", response);
        return response;
    }

    @Override
    public String processRefund(String transactionId, BigDecimal refundAmount) throws Exception {
        log.info("Processing refund - Transaction ID: {}, Amount: {}", transactionId, refundAmount);

        // Simulate refund processing delay
        Thread.sleep(500);

        // Simulate refund processing
        String refundTransactionId = UUID.randomUUID().toString();
        String response = String.format("Refund successful. Refund Transaction ID: %s", refundTransactionId);

        log.info("Refund processed successfully: {}", response);
        return response;
    }
}
