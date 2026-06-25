package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.passenger.PassengerRequest;
import com.airlinebookingsystem.dto.passenger.PassengerResponse;
import com.airlinebookingsystem.service.PassengerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for passenger operations.
 * All endpoints require authentication.
 * Exception handling delegated to GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/passengers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Passengers", description = "Manage passengers within bookings — add, update, assign seats, delete")
public class PassengerController {

    private final PassengerService passengerService;

    @Operation(summary = "Get all passengers", description = "Returns all passengers across all bookings")
    @GetMapping
    public ResponseEntity<List<PassengerResponse>> getAllPassengers() {
        log.info("GET /passengers");
        return ResponseEntity.ok(passengerService.getAllPassengers());
    }

    @Operation(summary = "Get passenger by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Passenger found"),
            @ApiResponse(responseCode = "404", description = "Passenger not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PassengerResponse> getPassengerById(
            @Parameter(description = "Passenger ID") @PathVariable @NonNull Long id) {
        log.info("GET /passengers/{}", id);
        return ResponseEntity.ok(passengerService.getPassengerById(id));
    }

    @Operation(summary = "Get all passengers for a booking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Passengers returned"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PassengerResponse>> getPassengersByBookingId(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId) {
        log.info("GET /passengers/booking/{}", bookingId);
        return ResponseEntity.ok(passengerService.getPassengersByBookingId(bookingId));
    }

    @Operation(summary = "Get all passengers on a flight")
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<PassengerResponse>> getPassengersByFlightId(
            @Parameter(description = "Flight ID") @PathVariable Long flightId) {
        log.info("GET /passengers/flight/{}", flightId);
        return ResponseEntity.ok(passengerService.getPassengersByFlightId(flightId));
    }

    @Operation(summary = "Look up passengers by passport number")
    @GetMapping("/passport/{passportNumber}")
    public ResponseEntity<List<PassengerResponse>> getPassengersByPassportNumber(
            @Parameter(description = "Passport number") @PathVariable String passportNumber) {
        log.info("GET /passengers/passport/{}", passportNumber);
        return ResponseEntity.ok(passengerService.getPassengersByPassportNumber(passportNumber));
    }

    @Operation(summary = "Get passenger count for a booking")
    @GetMapping("/booking/{bookingId}/count")
    public ResponseEntity<Long> getPassengerCountByBooking(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId) {
        log.info("GET /passengers/booking/{}/count", bookingId);
        return ResponseEntity.ok(passengerService.getPassengerCountByBooking(bookingId));
    }

    @Operation(summary = "Add a passenger to a booking", description = "Adds a single passenger to an existing PENDING booking")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Passenger added"),
            @ApiResponse(responseCode = "400", description = "Booking is not PENDING, or invalid passenger data"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate passport number or seat already assigned")
    })
    @PostMapping("/booking/{bookingId}")
    public ResponseEntity<PassengerResponse> addPassengerToBooking(
            @Parameter(description = "Booking ID") @PathVariable @NonNull Long bookingId,
            @RequestBody PassengerRequest request) {
        log.info("POST /passengers/booking/{}", bookingId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(passengerService.createPassenger(request, bookingId));
    }

    @Operation(summary = "Update a passenger's details", description = "Updates personal details for an existing passenger. Booking must be PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Passenger updated"),
            @ApiResponse(responseCode = "400", description = "Booking is CONFIRMED or CANCELLED"),
            @ApiResponse(responseCode = "404", description = "Passenger not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PassengerResponse> updatePassenger(
            @Parameter(description = "Passenger ID") @PathVariable @NonNull Long id,
            @RequestBody PassengerRequest request) {
        log.info("PUT /passengers/{}", id);
        return ResponseEntity.ok(passengerService.updatePassenger(id, request));
    }

    @Operation(summary = "Assign a seat to a passenger", description = "Assigns a specific seat number to a passenger e.g. 14A, 2C")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seat assigned"),
            @ApiResponse(responseCode = "404", description = "Passenger not found"),
            @ApiResponse(responseCode = "409", description = "Seat already assigned to another passenger")
    })
    @PatchMapping("/{id}/seat")
    public ResponseEntity<PassengerResponse> assignSeat(
            @Parameter(description = "Passenger ID") @PathVariable @NonNull Long id,
            @Parameter(description = "Seat number e.g. 14A") @RequestParam String seatNumber) {
        log.info("PATCH /passengers/{}/seat — seat: {}", id, seatNumber);
        return ResponseEntity.ok(passengerService.assignSeat(id, seatNumber));
    }

    @Operation(summary = "Remove a passenger from a booking", description = "Deletes a passenger. Booking must be in PENDING status.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Passenger deleted"),
            @ApiResponse(responseCode = "400", description = "Booking is CONFIRMED or CANCELLED"),
            @ApiResponse(responseCode = "404", description = "Passenger not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePassenger(
            @Parameter(description = "Passenger ID") @PathVariable @NonNull Long id) {
        log.info("DELETE /passengers/{}", id);
        passengerService.deletePassenger(id);
        return ResponseEntity.noContent().build();
    }
}