package com.airlinebookingsystem.service.impl;

import com.airlinebookingsystem.entity.Payment;
import com.airlinebookingsystem.service.PaymentGatewayService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock implementation of PaymentGatewayService for development/testing.
 * Replace it with actual payment gateway integration in production.
 *
 * <p>Rejections are opt-in via {@code payments.gateway.simulated-failure-rate}
 * and off by default. This previously failed 10% of charges at random, which
 * meant roughly one booking in ten broke for no reason a tester could see —
 * and on a multi-leg itinerary, closer to one in five. A failure path worth
 * exercising is worth triggering deliberately.
 */
@Service
@Slf4j
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    @Value("${payments.gateway.simulated-failure-rate:0.0}")
    private double simulatedFailureRate;

    @Value("${payments.gateway.simulated-latency-ms:600}")
    private long simulatedLatencyMs;

    @PostConstruct
    void logConfiguration() {
        if (simulatedFailureRate > 0) {
            log.warn("Mock payment gateway will reject {}% of charges — set "
                            + "payments.gateway.simulated-failure-rate to 0 to disable",
                    Math.round(simulatedFailureRate * 100));
        }
    }

    @Override
    public String processPayment(String transactionId, BigDecimal amount,
                                 Payment.PaymentMethod paymentMethod,
                                 Object paymentDetails) throws Exception {

        log.info("Processing payment - Transaction ID: {}, Amount: {}, Method: {}",
                transactionId, amount, paymentMethod);

        // Simulate payment processing delay
        Thread.sleep(simulatedLatencyMs);

        if (simulatedFailureRate > 0
                && ThreadLocalRandom.current().nextDouble() < simulatedFailureRate) {
            log.warn("Simulating a gateway rejection for transaction {}", transactionId);
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

        // Refunds are never simulated as failing: a rejected refund would strand
        // the caller's compensating rollback with no way to recover.
        Thread.sleep(Math.min(simulatedLatencyMs, 500));

        // Simulate refund processing
        String refundTransactionId = UUID.randomUUID().toString();
        String response = String.format("Refund successful. Refund Transaction ID: %s", refundTransactionId);

        log.info("Refund processed successfully: {}", response);
        return response;
    }
}
