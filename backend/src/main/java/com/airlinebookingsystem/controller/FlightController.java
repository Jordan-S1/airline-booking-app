package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.common.PagedResponse;
import com.airlinebookingsystem.dto.flight.*;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Flights", description = "Search flights and manage flight records")
public class FlightController {

    private final FlightService flightService;

    /**
     * The flights collection, always a page.
     *
     * <p>There is deliberately no unpaged variant. This returned every flight
     * until the timetable reached several thousand rows, at which point one
     * anonymous request meant a 2 MB response and — the airline and both
     * airports being lazy — roughly eleven thousand queries to build it. A
     * collection endpoint that gets slower every day the schedule rolls forward
     * is not one worth keeping alongside this.
     */
    @Operation(summary = "Get a page of flights",
            description = "Free-text search across flight number, either airport code and "
                    + "airline name, newest departures first. An empty search matches "
                    + "everything. Page size is capped server-side.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<PagedResponse<FlightResponse>> getFlights(
            @Parameter(description = "Free-text term; blank matches all")
            @RequestParam(defaultValue = "") String search,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Rows per page, capped at 100")
            @RequestParam(defaultValue = "40") int size) {
        log.info("GET /flights — search '{}', page {}, size {}", search, page, size);
        return ResponseEntity.ok(flightService.searchFlightsPaged(search, page, size));
    }

    @Operation(summary = "Get flight by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight found"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<FlightResponse> getFlightById(
            @Parameter(description = "Flight ID") @PathVariable Long id) {
        log.info("GET /flights/{}", id);
        return ResponseEntity.ok(flightService.getFlightById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", id)));
    }

    @Operation(summary = "Get flight by flight number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight found"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @SecurityRequirements
    @GetMapping("/number/{flightNumber}")
    public ResponseEntity<FlightResponse> getFlightByNumber(
            @Parameter(description = "Flight number e.g. EI204") @PathVariable @NotBlank String flightNumber) {
        log.info("GET /flights/number/{}", flightNumber);
        return ResponseEntity.ok(flightService.getFlightByNumber(flightNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", flightNumber)));
    }

    @Operation(summary = "Get live status for a flight",
            description = "Returns the flight's current status and progress, derived from its timetable at request time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status returned"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @SecurityRequirements
    @GetMapping("/{id}/status")
    public ResponseEntity<FlightStatusResponse> getFlightStatus(
            @Parameter(description = "Flight ID") @PathVariable Long id) {
        log.info("GET /flights/{}/status", id);
        return ResponseEntity.ok(flightService.getFlightStatus(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", id)));
    }

    @Operation(summary = "Search available flights",
            description = "Search by origin, destination, date, passengers, and seat class")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "404", description = "Airport not found")
    })
    @SecurityRequirements
    @PostMapping("/search")
    public ResponseEntity<FlightSearchResult> searchFlights(
            @Valid @RequestBody FlightSearchRequest request) {
        log.info("POST /flights/search: {} → {} on {}", request.departureAirport(), request.arrivalAirport(), request.departureDate());
        return ResponseEntity.ok(flightService.searchFlights(request));
    }

    @Operation(summary = "Search a multi-city itinerary",
            description = "Searches each leg independently (2-5 legs). Returns one result set per leg, in submission order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Fewer than two legs, or a leg is invalid"),
            @ApiResponse(responseCode = "404", description = "Airport not found")
    })
    @SecurityRequirements
    @PostMapping("/search/multi-city")
    public ResponseEntity<MultiCitySearchResult> searchMultiCity(
            @Valid @RequestBody MultiCitySearchRequest request) {
        log.info("POST /flights/search/multi-city — {} legs", request.legs().size());
        return ResponseEntity.ok(flightService.searchMultiCity(request));
    }

    @Operation(summary = "Get upcoming flights")
    @SecurityRequirements
    @GetMapping("/upcoming")
    public ResponseEntity<List<FlightSearchResponse>> getUpcomingFlights() {
        log.info("GET /flights/upcoming");
        return ResponseEntity.ok(flightService.getUpcomingFlights());
    }

    @Operation(summary = "Get upcoming arrivals at an airport",
            description = "Upcoming flights arriving at the given airport from any origin, earliest first.")
    @SecurityRequirements
    @GetMapping("/arrivals/{airportCode}")
    public ResponseEntity<List<FlightSearchResponse>> getUpcomingArrivals(
            @Parameter(description = "IATA airport code e.g. CDG") @PathVariable @NotBlank String airportCode) {
        log.info("GET /flights/arrivals/{}", airportCode);
        return ResponseEntity.ok(flightService.getUpcomingArrivals(airportCode));
    }

    @Operation(summary = "Get flights by airline code")
    @SecurityRequirements
    @GetMapping("/airline/{airlineCode}")
    public ResponseEntity<List<FlightSearchResponse>> getFlightsByAirlineCode(
            @Parameter(description = "IATA airline code e.g. EI") @PathVariable @NotBlank String airlineCode) {
        log.info("GET /flights/airline/{}", airlineCode);
        return ResponseEntity.ok(flightService.getFlightsByAirlineCode(airlineCode));
    }

    @Operation(summary = "Create a new flight",
            description = "References airline and airports by IATA code. Duration is auto-calculated from departure/arrival times.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Flight created"),
            @ApiResponse(responseCode = "400", description = "Validation failed or same departure/arrival airport"),
            @ApiResponse(responseCode = "404", description = "Airline or airport not found"),
            @ApiResponse(responseCode = "409", description = "Flight number already exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FlightResponse> createFlight(@Valid @RequestBody FlightRequest request) {
        log.info("POST /flights — number: {}", request.flightNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlight(request));
    }

    @Operation(summary = "Update a flight", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight updated"),
            @ApiResponse(responseCode = "404", description = "Flight or airport not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<FlightResponse> updateFlight(
            @PathVariable Long id,
            @Valid @RequestBody FlightRequest request) {
        log.info("PUT /flights/{}", id);
        return ResponseEntity.ok(flightService.updateFlight(id, request));
    }

    @Operation(summary = "Delete a flight", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Flight deleted"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        log.info("DELETE /flights/{}", id);
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }
}
