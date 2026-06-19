package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.FlightSearchRequest;
import com.airlinebookingsystem.dto.FlightSearchResponse;
import com.airlinebookingsystem.dto.FlightSearchResult;
import com.airlinebookingsystem.entity.Flight;
import com.airlinebookingsystem.service.FlightService;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;

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
public class FlightController {

    private final FlightService flightService;

    /**
     * Retrieves all flights from the system.
     *
     * @return ResponseEntity containing a list of all flights
     */
    @GetMapping
    public ResponseEntity<List<Flight>> getAllFlights() {
        log.info("GET /flights");
        return ResponseEntity.ok(flightService.getAllFlights());
    }

    /**
     * Retrieves a flight by its ID.
     *
     * @param id the ID of the flight to retrieve
     * @return ResponseEntity containing the flight if found, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Flight> getFlightById(@PathVariable Long id) {
        log.info("GET /flights/{}", id);
        return ResponseEntity.ok(flightService.getFlightById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", id)));
    }

    /**
     * Retrieves a flight by its flight number.
     *
     * @param flightNumber the flight number to search for (must not be blank)
     * @return ResponseEntity containing the flight if found, or 404 if not found
     */
    @GetMapping("/number/{flightNumber}")
    public ResponseEntity<Flight> getFlightByNumber(@PathVariable @NotBlank String flightNumber) {
        log.info("GET /flights/number/{}", flightNumber);
        return ResponseEntity.ok(flightService.getFlightByNumber(flightNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", flightNumber)));
    }

    /**
     * Searches for available flights based on search criteria.
     * Supports both one-way and round-trip searches.
     * Request body should contain:
     * - departureAirport: Airport code (e.g., "LAX")
     * - arrivalAirport: Airport code (e.g., "JFK")
     * - departureDate: Departure date in YYYY-MM-DD format
     * - returnDate: Return date (optional, for round-trip)
     * - passengers: Number of passengers (optional, defaults to 1)
     * - seatClass: Seat class preference (optional, defaults to "ECONOMY")
     * - directFlightsOnly: Filter for direct flights only (optional, defaults to
     * false)
     *
     * @param request the FlightSearchRequest DTO with search criteria
     * @return ResponseEntity containing FlightSearchResult with outbound and
     *         optional return flights
     */
    @PostMapping("/search")
    public ResponseEntity<FlightSearchResult> searchFlights(@Valid @RequestBody FlightSearchRequest request) {
        log.info(
                "POST /flights/search: {} -> {} on {}",
                request.departureAirport(),
                request.arrivalAirport(),
                request.departureDate());
        return ResponseEntity.ok(flightService.searchFlights(request));
    }

    /**
     * Retrieves all upcoming flights from the current date and time.
     *
     * @return ResponseEntity containing a list of upcoming flights
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<FlightSearchResponse>> getUpcomingFlights() {
        log.info("GET /flights/upcoming");
        return ResponseEntity.ok(flightService.getUpcomingFlights());
    }

    /**
     * Retrieves all flights operated by a specific airline using airline code.
     *
     * @param airlineCode the code of the airline (e.g., "AA", "DL", "UA") - must
     *                    not be blank
     * @return ResponseEntity containing a list of FlightSearchResponse DTOs for the
     *         specified airline
     */
    @GetMapping("/airline/{airlineCode}")
    public ResponseEntity<List<FlightSearchResponse>> getFlightsByAirlineCode(
            @PathVariable @NotBlank String airlineCode) {
        log.info("GET /flights/airline/{}", airlineCode);
        return ResponseEntity.ok(flightService.getFlightsByAirlineCode(airlineCode));
    }

    /**
     * Creates a new flight in the system.
     *
     * @param flight the flight to create
     * @return ResponseEntity containing the created flight with status 201
     */
    @PostMapping
    public ResponseEntity<Flight> createFlight(@Valid @RequestBody Flight flight) {
        log.info("POST /flights: {}", flight.getFlightNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlight(flight));
    }

    /**
     * Updates an existing flight's details.
     *
     * @param id            the ID of the flight to update
     * @param flightDetails the new flight details
     * @return ResponseEntity containing the updated flight
     */
    @PutMapping("/{id}")
    public ResponseEntity<Flight> updateFlight(@PathVariable Long id,
            @Valid @RequestBody Flight flightDetails) {
        log.info("PUT /flights/{}", id);
        return ResponseEntity.ok(flightService.updateFlight(id, flightDetails));
    }

    /**
     * Deletes a flight from the system.
     *
     * @param id the ID of the flight to delete
     * @return ResponseEntity with status 204 if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        log.info("DELETE /flights/{}", id);
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }
}
