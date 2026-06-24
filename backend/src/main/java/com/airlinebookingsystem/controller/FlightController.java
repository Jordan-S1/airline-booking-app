package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.flight.FlightSearchRequest;
import com.airlinebookingsystem.dto.flight.FlightSearchResponse;
import com.airlinebookingsystem.dto.flight.FlightSearchResult;
import com.airlinebookingsystem.entity.Flight;
import com.airlinebookingsystem.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.NonNull;
import java.util.List;

/**
 * REST Controller for managing flight operations in the airline booking system.
 * Provides endpoints for flight search, CRUD operations, and flight-specific
 * queries.
 */
@RestController
@RequestMapping("api/v1/flights")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Flights", description = "Search flights and manage flight records")
public class FlightController {

    private final FlightService flightService;

    @Operation(summary = "Get all flights")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<Flight>> getAllFlights() {
        log.info("GET /flights");
        return ResponseEntity.ok(flightService.getAllFlights());
    }

    @Operation(summary = "Get flight by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight found"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<Flight> getFlightById(@Parameter(description = "Flight ID") @PathVariable @NonNull Long id) {
        log.info("GET /flights/{}", id);
        return ResponseEntity.ok(flightService.getFlightById(id));
    }

    @Operation(summary = "Get flight by flight number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight found"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @SecurityRequirements
    @GetMapping("/number/{flightNumber}")
    public ResponseEntity<Flight> getFlightByNumber(
            @Parameter(description = "Flight number e.g. EI204") @PathVariable @NotBlank String flightNumber) {
        log.info("GET /flights/number/{}", flightNumber);
        return ResponseEntity.ok(flightService.getFlightByNumber(flightNumber));
    }

    @Operation(summary = "Search available flights", description = "Search by origin, destination, date, passengers, and seat class")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    @SecurityRequirements
    @PostMapping("/search")
    public ResponseEntity<FlightSearchResult> searchFlights(@Valid @RequestBody FlightSearchRequest request) {
        log.info(
                "POST /flights/search: {} -> {} on {}",
                request.departureAirport(),
                request.arrivalAirport(),
                request.departureDate());
        return ResponseEntity.ok(flightService.searchFlights(request));
    }

    @Operation(summary = "Get upcoming flights")
    @SecurityRequirements
    @GetMapping("/upcoming")
    public ResponseEntity<List<FlightSearchResponse>> getUpcomingFlights() {
        log.info("GET /flights/upcoming");
        return ResponseEntity.ok(flightService.getUpcomingFlights());
    }

    @Operation(summary = "Get flights by airline code")
    @SecurityRequirements
    @GetMapping("/airline/{airlineCode}")
    public ResponseEntity<List<FlightSearchResponse>> getFlightsByAirlineCode(
            @Parameter(description = "IATA airline code e.g. EI") @PathVariable @NotBlank String airlineCode) {
        log.info("GET /flights/airline/{}", airlineCode);
        return ResponseEntity.ok(flightService.getFlightsByAirlineCode(airlineCode));
    }

    @Operation(summary = "Create a new flight", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Flight created"),
            @ApiResponse(responseCode = "400", description = "Invalid flight data"),
            @ApiResponse(responseCode = "409", description = "Flight number already exists")
    })
    @PostMapping
    public ResponseEntity<Flight> createFlight(@Valid @RequestBody Flight flight) {
        log.info("POST /flights: {}", flight.getFlightNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlight(flight));
    }

    @Operation(summary = "Update a flight", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight updated"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Flight> updateFlight(@PathVariable @NonNull Long id,
            @Valid @RequestBody Flight flightDetails) {
        log.info("PUT /flights/{}", id);
        return ResponseEntity.ok(flightService.updateFlight(id, flightDetails));
    }

    @Operation(summary = "Delete a flight", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Flight deleted"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable @NonNull Long id) {
        log.info("DELETE /flights/{}", id);
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }
}
