package com.airlinebookingsystem.service;

import com.airlinebookingsystem.entity.Payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service interface for payment gateway operations.
 * Implement this interface to integrate with different payment providers.
 */
public interface PaymentGatewayService {

    /**
     * Processes a payment through the payment gateway.
     *
     * @param transactionId unique transaction identifier
     * @param amount payment amount
     * @param paymentMethod payment method used
     * @param paymentDetails additional payment details (card info, etc.)
     * @return gateway response message
     * @throws Exception if payment processing fails
     */
    String processPayment(String transactionId, BigDecimal amount,
                          Payment.PaymentMethod paymentMethod,
                          Map<String, String> paymentDetails) throws Exception;

    /**
     * Processes a refund through the payment gateway.
     *
     * @param transactionId original transaction identifier
     * @param refundAmount amount to refund
     * @return gateway response message
     * @throws Exception if refund processing fails
     */
    String processRefund(String transactionId, BigDecimal refundAmount) throws Exception;
}
