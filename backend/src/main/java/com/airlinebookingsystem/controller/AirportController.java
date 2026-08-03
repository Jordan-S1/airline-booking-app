package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.airport.AirportRequest;
import com.airlinebookingsystem.dto.airport.AirportResponse;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.service.AirportService;
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
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Airports", description = "Search and manage airport data")
public class AirportController {

    private final AirportService airportService;

    @Operation(summary = "Get all airports")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<AirportResponse>> getAllAirports() {
        log.info("GET /airports");
        return ResponseEntity.ok(airportService.getAllAirports());
    }

    @Operation(summary = "Search airports by name, city, or code")
    @SecurityRequirements
    @GetMapping("/search")
    public ResponseEntity<List<AirportResponse>> searchAirports(
            @Parameter(description = "Search term e.g. Dublin, DUB, London")
            @RequestParam String query) {
        log.info("GET /airports/search?query={}", query);
        return ResponseEntity.ok(airportService.searchAirports(query));
    }

    @Operation(summary = "Get airports by country")
    @SecurityRequirements
    @GetMapping("/by-country")
    public ResponseEntity<List<AirportResponse>> getAirportsByCountry(
            @Parameter(description = "Country name e.g. Ireland")
            @RequestParam String country) {
        log.info("GET /airports/by-country?country={}", country);
        return ResponseEntity.ok(airportService.getAirportsByCountry(country));
    }

    @Operation(summary = "Get airport by IATA code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Airport found"),
            @ApiResponse(responseCode = "404", description = "Airport not found")
    })
    @SecurityRequirements
    @GetMapping("/{code}")
    public ResponseEntity<AirportResponse> getAirportByCode(
            @Parameter(description = "IATA code e.g. DUB") @PathVariable String code) {
        log.info("GET /airports/{}", code);
        return ResponseEntity.ok(airportService.getAirportByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Airport", code)));
    }

    @Operation(summary = "Create a new airport",
            description = "ADMIN only")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Airport created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AirportResponse> createAirport(@Valid @RequestBody AirportRequest request) {
        log.info("POST /airports — code: {}", request.code());
        return ResponseEntity.status(HttpStatus.CREATED).body(airportService.saveAirport(request));
    }

    @Operation(summary = "Update an airport",
            description = "ADMIN only. Note: IATA code cannot be changed — delete and recreate if needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Airport updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Airport not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<AirportResponse> updateAirport(
            @Parameter(description = "Airport ID") @PathVariable Long id,
            @Valid @RequestBody AirportRequest request) {
        log.info("PUT /airports/{}", id);
        return ResponseEntity.ok(airportService.updateAirport(id, request));
    }

    @Operation(summary = "Delete an airport",
            description = "ADMIN only. Cannot delete airports that have associated flights.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Airport deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Airport not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirport(
            @Parameter(description = "Airport ID") @PathVariable Long id) {
        log.info("DELETE /airports/{}", id);
        airportService.deleteAirport(id);
        return ResponseEntity.noContent().build();
    }
}