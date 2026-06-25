package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.booking.BookingRequest;
import com.airlinebookingsystem.dto.booking.BookingResponse;
import com.airlinebookingsystem.dto.passenger.PassengerResponse;
import com.airlinebookingsystem.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for booking operations.
 * All endpoints require authentication — no @SecurityRequirements override
 * needed.
 * Exception handling delegated to GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Bookings", description = "Create, view, confirm, cancel, and update bookings")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Create a new booking", description = "Creates a PENDING booking for the given flight and passengers")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or seat class"),
            @ApiResponse(responseCode = "404", description = "Flight or user not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient seats available")
    })
    @PostMapping("/user/{userId}")
    public ResponseEntity<BookingResponse> createBooking(
            @Parameter(description = "ID of the user making the booking") @PathVariable @NonNull Long userId,
            @Valid @RequestBody BookingRequest request) {
        log.info("POST /bookings/user/{}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, userId));
    }

    @Operation(summary = "Get booking by reference", description = "Returns full booking details for the given reference e.g. BK17234567890001")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{bookingReference}")
    public ResponseEntity<BookingResponse> getBookingByReference(
            @Parameter(description = "Booking reference number") @PathVariable String bookingReference) {
        log.info("GET /bookings/{}", bookingReference);
        return ResponseEntity.ok(bookingService.getBookingByReference(bookingReference));
    }

    @Operation(summary = "Get all bookings for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings returned"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByUserId(
            @Parameter(description = "User ID") @PathVariable @NonNull Long userId) {
        log.info("GET /bookings/user/{}", userId);
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    @Operation(summary = "Get bookings by status", description = "Valid statuses: PENDING, CONFIRMED, CANCELLED, COMPLETED")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BookingResponse>> getBookingsByStatus(
            @Parameter(description = "Booking status") @PathVariable String status) {
        log.info("GET /bookings/status/{}", status);
        return ResponseEntity.ok(bookingService.getBookingsByStatus(status));
    }

    @Operation(summary = "Get passengers for a booking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Passengers returned"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{bookingReference}/passengers")
    public ResponseEntity<List<PassengerResponse>> getBookingPassengers(
            @Parameter(description = "Booking reference number") @PathVariable String bookingReference) {
        log.info("GET /bookings/{}/passengers", bookingReference);
        return ResponseEntity.ok(bookingService.getBookingPassengers(bookingReference));
    }

    @Operation(summary = "Confirm a booking", description = "Moves a PENDING booking to CONFIRMED status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking confirmed"),
            @ApiResponse(responseCode = "400", description = "Booking is already cancelled"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PatchMapping("/{bookingReference}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @Parameter(description = "Booking reference number") @PathVariable String bookingReference) {
        log.info("PATCH /bookings/{}/confirm", bookingReference);
        return ResponseEntity.ok(bookingService.confirmBooking(bookingReference));
    }

    @Operation(summary = "Cancel a booking", description = "Cancels the booking and restores seat availability on the flight")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled"),
            @ApiResponse(responseCode = "400", description = "Booking is already cancelled"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PatchMapping("/{bookingReference}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @Parameter(description = "Booking reference number") @PathVariable String bookingReference) {
        log.info("PATCH /bookings/{}/cancel", bookingReference);
        return ResponseEntity.ok(bookingService.cancelBooking(bookingReference));
    }

    @Operation(summary = "Update a booking", description = "Only PENDING bookings can be updated. Allows changing seat class.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking updated"),
            @ApiResponse(responseCode = "400", description = "Booking is not in PENDING status"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient seats for new class")
    })
    @PutMapping("/{bookingReference}")
    public ResponseEntity<BookingResponse> updateBooking(
            @Parameter(description = "Booking reference number") @PathVariable String bookingReference,
            @Valid @RequestBody BookingRequest request) {
        log.info("PUT /bookings/{}", bookingReference);
        return ResponseEntity.ok(bookingService.updateBooking(bookingReference, request));
    }
}