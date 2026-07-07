package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.airline.AirlineRequest;
import com.airlinebookingsystem.dto.airline.AirlineResponse;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.service.AirlineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/airlines")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Airlines", description = "Manage airlines — CRUD, activate/deactivate")
public class AirlineController {

    private final AirlineService airlineService;

    @Operation(summary = "Get all airlines")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<AirlineResponse>> getAllAirlines() {
        log.info("GET /airlines");
        return ResponseEntity.ok(airlineService.getAllAirlines());
    }

    @Operation(summary = "Get all active airlines")
    @SecurityRequirements
    @GetMapping("/active")
    public ResponseEntity<List<AirlineResponse>> getAllActiveAirlines() {
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
    public ResponseEntity<AirlineResponse> getAirlineById(
            @Parameter(description = "Airline ID") @PathVariable Long id) {
        log.info("GET /airlines/{}", id);
        return ResponseEntity.ok(airlineService.getAirlineById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", id)));
    }

    @Operation(summary = "Get airline by IATA code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Airline found"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @SecurityRequirements
    @GetMapping("/code/{code}")
    public ResponseEntity<AirlineResponse> getAirlineByCode(
            @Parameter(description = "IATA code e.g. EI for Aer Lingus") @PathVariable String code) {
        log.info("GET /airlines/code/{}", code);
        return ResponseEntity.ok(airlineService.getAirlineByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", code)));
    }

    @Operation(summary = "Get airlines by country")
    @SecurityRequirements
    @GetMapping("/country/{country}")
    public ResponseEntity<List<AirlineResponse>> getAirlinesByCountry(
            @Parameter(description = "Country name e.g. Ireland") @PathVariable String country) {
        log.info("GET /airlines/country/{}", country);
        return ResponseEntity.ok(airlineService.getAirlinesByCountry(country));
    }

    @Operation(summary = "Create a new airline", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Airline created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Airline code already exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AirlineResponse> createAirline(@Valid @RequestBody AirlineRequest request) {
        log.info("POST /airlines — code: {}", request.code());
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.createAirline(request));
    }

    @Operation(summary = "Update an airline", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Airline updated"),
            @ApiResponse(responseCode = "404", description = "Airline not found"),
            @ApiResponse(responseCode = "409", description = "Airline code conflict")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<AirlineResponse> updateAirline(
            @PathVariable Long id,
            @Valid @RequestBody AirlineRequest request) {
        log.info("PUT /airlines/{}", id);
        return ResponseEntity.ok(airlineService.updateAirline(id, request));
    }

    @Operation(summary = "Deactivate an airline (soft delete)", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Airline deactivated"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateAirline(@PathVariable Long id) {
        log.info("PATCH /airlines/{}/deactivate", id);
        airlineService.deactivateAirline(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivate a deactivated airline", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Airline reactivated"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateAirline(@PathVariable Long id) {
        log.info("PATCH /airlines/{}/reactivate", id);
        airlineService.reactivateAirline(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Permanently delete an airline", description = "Requires authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Airline deleted"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirline(@PathVariable Long id) {
        log.info("DELETE /airlines/{}", id);
        airlineService.deleteAirline(id);
        return ResponseEntity.noContent().build();
    }
}
