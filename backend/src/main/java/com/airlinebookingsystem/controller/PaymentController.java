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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for payment operations.
 * Role access rules:
 *   ADMIN        — full access to all endpoints
 *   AIRLINE_STAFF — read-only: view payments by transaction, booking, status, date range
 *   CUSTOMER     — can only process payment and refund for their own bookings
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Payments", description = "Process and manage payments for bookings")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Process a payment",
            description = "Processes payment for a PENDING booking. CUSTOMER can only pay for their own bookings.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "400", description = "Booking not in PENDING status or amount mismatch"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Payment already completed for this booking")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("POST /payments — bookingId: {}", request.bookingId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(request));
    }

    @Operation(summary = "Get payment by transaction ID",
            description = "ADMIN and AIRLINE_STAFF only")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'AIRLINE_STAFF')")
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(
            @Parameter(description = "Transaction ID") @PathVariable String transactionId) {
        log.info("GET /payments/transaction/{}", transactionId);
        return ResponseEntity.ok(paymentService.getPaymentByTransactionId(transactionId));
    }

    @Operation(summary = "Get payment for a booking",
            description = "ADMIN and AIRLINE_STAFF can see any booking's payment. CUSTOMER can only see their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Payment not found for this booking")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'AIRLINE_STAFF', 'CUSTOMER')")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId) {
        log.info("GET /payments/booking/{}", bookingId);
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    @Operation(summary = "Get payments by status — ADMIN only",
            description = "Valid statuses: PENDING, SUCCESS, FAILED, REFUNDED")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(
            @Parameter(description = "Payment status") @PathVariable String status) {
        log.info("GET /payments/status/{}", status);
        Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(paymentStatus));
    }

    @Operation(summary = "Get payments within a date range — ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/date-range")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByDateRange(
            @Parameter(description = "Start date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("GET /payments/date-range: {} to {}", startDate, endDate);
        return ResponseEntity.ok(paymentService.getPaymentsByDateRange(startDate, endDate));
    }

    @Operation(summary = "Refund a payment",
            description = "Issues a full refund and cancels the booking. CUSTOMER can only refund their own bookings.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund processed and booking cancelled"),
            @ApiResponse(responseCode = "400", description = "Payment cannot be refunded"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(description = "Transaction ID to refund") @PathVariable String transactionId) {
        log.info("POST /payments/{}/refund", transactionId);
        return ResponseEntity.ok(paymentService.refundPayment(transactionId, null));
    }
}