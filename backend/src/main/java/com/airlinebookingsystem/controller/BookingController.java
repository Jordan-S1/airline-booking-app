package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.booking.BookingRequest;
import com.airlinebookingsystem.dto.booking.BookingResponse;
import com.airlinebookingsystem.dto.booking.BookingUpdateRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for booking operations.
 * Role access rules:
 *   ADMIN  — full access to all endpoints
 *   CUSTOMER — can only manage their own bookings (enforced at service level)
 * Booking flow:
 *   1. POST /bookings/user/{userId}       — create booking (passengers optional)
 *   2. POST /passengers/booking/{id}      — add passengers individually
 *   3. PATCH /bookings/{ref}/confirm      — confirm booking
 *   4. POST /payments                     — process payment
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Bookings", description = "Create, view, confirm, cancel and update bookings")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Create a new booking",
            description = "ADMIN can create for any user. CUSTOMER can only create for themselves.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Flight or user not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient seats available")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @PostMapping("/user/{userId}")
    public ResponseEntity<BookingResponse> createBooking(
            @Parameter(description = "ID of the user making the booking") @PathVariable Long userId,
            @Valid @RequestBody BookingRequest request) {
        log.info("POST /bookings/user/{}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, userId));
    }

    @Operation(summary = "Get booking by reference",
            description = "ADMIN sees any booking. CUSTOMER sees only their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/{bookingReference}")
    public ResponseEntity<BookingResponse> getBookingByReference(
            @Parameter(description = "Booking reference e.g. BK17234567890001")
            @PathVariable String bookingReference) {
        log.info("GET /bookings/{}", bookingReference);
        return ResponseEntity.ok(bookingService.getBookingByReference(bookingReference));
    }

    @Operation(summary = "Get all bookings for a user",
            description = "ADMIN sees any user's bookings. CUSTOMER can only see their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings returned"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByUserId(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.info("GET /bookings/user/{}", userId);
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    @Operation(summary = "Get bookings by status — ADMIN only",
            description = "Valid statuses: PENDING, CONFIRMED, CANCELLED, COMPLETED")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BookingResponse>> getBookingsByStatus(
            @Parameter(description = "Booking status") @PathVariable String status) {
        log.info("GET /bookings/status/{}", status);
        return ResponseEntity.ok(bookingService.getBookingsByStatus(status));
    }

    @Operation(summary = "Get passengers for a booking")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/{bookingReference}/passengers")
    public ResponseEntity<List<PassengerResponse>> getBookingPassengers(
            @PathVariable String bookingReference) {
        log.info("GET /bookings/{}/passengers", bookingReference);
        return ResponseEntity.ok(bookingService.getBookingPassengers(bookingReference));
    }

    @Operation(summary = "Confirm a booking",
            description = "Moves a PENDING booking to CONFIRMED. CUSTOMER can only confirm their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking confirmed"),
            @ApiResponse(responseCode = "400", description = "Booking is already cancelled"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @PatchMapping("/{bookingReference}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable String bookingReference) {
        log.info("PATCH /bookings/{}/confirm", bookingReference);
        return ResponseEntity.ok(bookingService.confirmBooking(bookingReference));
    }

    @Operation(summary = "Cancel a booking",
            description = "Cancels the booking and restores seat availability. CUSTOMER can only cancel their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled"),
            @ApiResponse(responseCode = "400", description = "Booking already cancelled"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @PatchMapping("/{bookingReference}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable String bookingReference) {
        log.info("PATCH /bookings/{}/cancel", bookingReference);
        return ResponseEntity.ok(bookingService.cancelBooking(bookingReference));
    }

    @Operation(summary = "Update a booking",
            description = "Updates seat class and/or flight on a PENDING booking. CUSTOMER can only update their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking updated"),
            @ApiResponse(responseCode = "400", description = "Booking is not PENDING"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient seats")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @PutMapping("/{bookingReference}")
    public ResponseEntity<BookingResponse> updateBooking(
            @Parameter(description = "Booking reference") @PathVariable String bookingReference,
            @Valid @RequestBody BookingUpdateRequest request) {
        log.info("PUT /bookings/{}", bookingReference);
        return ResponseEntity.ok(bookingService.updateBooking(bookingReference, request));
    }
}