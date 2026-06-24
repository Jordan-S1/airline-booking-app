package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.entity.Airline;
import com.airlinebookingsystem.service.AirlineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for managing airline operations in the airline booking
 * system.
 * Provides endpoints for CRUD operations and airline-specific queries.
 */
@RestController
@RequestMapping("api/v1/airlines")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Airlines", description = "Manage airlines — CRUD, activate/deactivate")
public class AirlineController {

    private final AirlineService airlineService;

    @Operation(summary = "Get all airlines")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<Airline>> getAllAirlines() {
        log.info("GET /airlines");
        return ResponseEntity.ok(airlineService.getAllAirlines());
    }

    @Operation(summary = "Get all active airlines")
    @SecurityRequirements
    @GetMapping("/active")
    public ResponseEntity<List<Airline>> getAllActiveAirlines() {
        log.info("GET /airlines/active");
        return ResponseEntity.ok(airlineService.getAllActiveAirlines());
    }

    @Operation(summary = "Get airline by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Airline found"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<Airline> getAirlineById(
            @Parameter(description = "Airline ID") @PathVariable @NonNull Long id) {
        log.info("GET /airlines/{}", id);
        return ResponseEntity.ok(airlineService.getAirlineById(id));
    }

    @Operation(summary = "Get airline by IATA code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Airline found"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @SecurityRequirements
    @GetMapping("/code/{code}")
    public ResponseEntity<Airline> getAirlineByCode(
            @Parameter(description = "IATA code e.g. EI for Aer Lingus") @PathVariable String code) {
        log.info("GET /airlines/code/{}", code);
        return ResponseEntity.ok(airlineService.getAirlineByCode(code));
    }

    @Operation(summary = "Get airlines by country")
    @SecurityRequirements
    @GetMapping("/country/{country}")
    public ResponseEntity<List<Airline>> getAirlinesByCountry(
            @Parameter(description = "Country name e.g. Ireland") @PathVariable String country) {
        log.info("GET /airlines/country/{}", country);
        return ResponseEntity.ok(airlineService.getAirlinesByCountry(country));
    }

    @Operation(summary = "Create a new airline", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Airline created"),
            @ApiResponse(responseCode = "409", description = "Airline code already exists")
    })
    @PostMapping
    public ResponseEntity<Airline> createAirline(@Valid @RequestBody Airline airline) {
        log.info("POST /airlines - code: {}", airline.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.createAirline(airline));
    }

    @Operation(summary = "Update an existing airline", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Airline updated"),
            @ApiResponse(responseCode = "404", description = "Airline not found"),
            @ApiResponse(responseCode = "409", description = "Airline code already exists")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Airline> updateAirline(@PathVariable @NonNull Long id,
            @Valid @RequestBody Airline airlineDetails) {
        log.info("PUT /airlines/{}", id);
        return ResponseEntity.ok(airlineService.updateAirline(id, airlineDetails));
    }

    @Operation(summary = "Deactivate an airline", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Airline deactivated"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateAirline(@PathVariable @NonNull Long id) {
        log.info("PATCH /airlines/{}/deactivate", id);
        airlineService.deactivateAirline(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivate an airline", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Airline reactivated"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateAirline(@PathVariable @NonNull Long id) {
        log.info("PATCH /airlines/{}/reactivate", id);
        airlineService.reactivateAirline(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Permanently delete an airline", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Airline deleted"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirline(@PathVariable @NonNull Long id) {
        log.info("DELETE /airlines/{}", id);
        airlineService.deleteAirline(id);
        return ResponseEntity.noContent().build();
    }
}
