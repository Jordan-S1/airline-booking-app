package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.payment.PaymentRequest;
import com.airlinebookingsystem.dto.payment.PaymentResponse;
import com.airlinebookingsystem.entity.Payment;
import com.airlinebookingsystem.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for payment operations.
 * All endpoints require authentication.
 * Exception handling delegated to GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Payments", description = "Process and manage payments for bookings")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Process a payment", description = "Processes payment for a PENDING booking and moves it to CONFIRMED on success")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "400", description = "Booking not in PENDING status"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Payment already completed for this booking")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("POST /payments — bookingId: {}", request.bookingId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(request));
    }

    @Operation(summary = "Get payment by transaction ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(
            @Parameter(description = "Transaction ID") @PathVariable String transactionId) {
        log.info("GET /payments/transaction/{}", transactionId);
        return ResponseEntity.ok(paymentService.getPaymentByTransactionId(transactionId));
    }

    @Operation(summary = "Get payment for a booking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found for this booking")
    })
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId) {
        log.info("GET /payments/booking/{}", bookingId);
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    @Operation(summary = "Get payments by status", description = "Valid statuses: PENDING, SUCCESS, FAILED, REFUNDED")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(
            @Parameter(description = "Payment status") @PathVariable String status) {
        log.info("GET /payments/status/{}", status);
        Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(paymentStatus));
    }

    @Operation(summary = "Get payments within a date range")
    @GetMapping("/date-range")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByDateRange(
            @Parameter(description = "Start date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("GET /payments/date-range: {} to {}", startDate, endDate);
        return ResponseEntity.ok(paymentService.getPaymentsByDateRange(startDate, endDate));
    }

    @Operation(summary = "Refund a payment", description = "Issues a full or partial refund for a completed payment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund processed"),
            @ApiResponse(responseCode = "400", description = "Payment cannot be refunded or refund amount exceeds original"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(description = "Transaction ID to refund") @PathVariable String transactionId,
            @Parameter(description = "Refund amount (must not exceed original payment)") @RequestParam BigDecimal amount) {
        log.info("POST /payments/{}/refund — amount: {}", transactionId, amount);
        return ResponseEntity.ok(paymentService.refundPayment(transactionId, amount));
    }
}