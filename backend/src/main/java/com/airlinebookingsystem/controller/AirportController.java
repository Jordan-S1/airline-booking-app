package com.airlinebookingsystem.controller;

/**
 * REST controller for managing airport-related operations in the airline booking system.
 * Provides endpoints for retrieving, searching, and creating airport records.
 * All endpoints return appropriate HTTP status codes and handle cross-origin requests.
 */

import com.airlinebookingsystem.entity.Airport;
import com.airlinebookingsystem.service.AirportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/airports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Airports", description = "Search and retrieve airport data")
public class AirportController {

    private final AirportService airportService;

    @Operation(summary = "Get all airports")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<Airport>> getAllAirports() {
        log.info("GET /airports");
        return ResponseEntity.ok(airportService.getAllAirports());
    }

    @Operation(summary = "Search airports by name, city, or code")
    @SecurityRequirements
    @GetMapping("/search")
    public ResponseEntity<List<Airport>> searchAirports(
            @Parameter(description = "Search term e.g. Dublin, DUB, London") @RequestParam String query) {
        log.info("GET /airports/search?query={}", query);
        return ResponseEntity.ok(airportService.searchAirports(query));
    }

    @Operation(summary = "Get airports by country")
    @SecurityRequirements
    @GetMapping("/by-country")
    public ResponseEntity<List<Airport>> getAirportsByCountry(
            @Parameter(description = "Country name e.g. Ireland") @RequestParam String country) {
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
    public ResponseEntity<Airport> getAirportByCode(
            @Parameter(description = "IATA code e.g. DUB") @PathVariable String code) {
        log.info("GET /airports/{}", code);
        return ResponseEntity.ok(airportService.getAirportByCode(code));
    }

    @Operation(summary = "Create a new airport", description = "Requires authentication")
    @ApiResponse(responseCode = "200", description = "Airport created")
    @PostMapping
    public ResponseEntity<Airport> createAirport(@RequestBody Airport airport) {
        log.info("POST /airports — code: {}", airport.getCode());
        return ResponseEntity.ok(airportService.saveAirport(airport));
    }
}
