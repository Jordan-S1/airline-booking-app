package com.airlinebookingsystem.controller;

/**
 * REST controller for managing airport-related operations in the airline booking system.
 * Provides endpoints for retrieving, searching, and creating airport records.
 * All endpoints return appropriate HTTP status codes and handle cross-origin requests.
 */

import com.airlinebookingsystem.entity.Airport;
import com.airlinebookingsystem.service.AirportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/airports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // For development

public class AirportController {

    private final AirportService airportService;

    /**
     * Retrieves all airports from the system.
     *
     * @return ResponseEntity containing a list of all airports with HTTP status 200 (OK)
     */
    @GetMapping
    public ResponseEntity<List<Airport>> getAllAirports() {
        return ResponseEntity.ok(airportService.getAllAirports());
    }

    /**
     * Searches for airports based on a query string.
     * Matches against airport name, city, or code.
     *
     * @param query the search string to match against airport fields
     * @return ResponseEntity containing a list of matching airports with HTTP status 200 (OK)
     */
    @GetMapping("/search")
    public ResponseEntity<List<Airport>> searchAirports(@RequestParam String query) {
        return ResponseEntity.ok(airportService.searchAirports(query));
    }

    /**
     * Retrieves a list of airports located in a specified country.
     *
     * @param country the name of the country to filter airports by
     * @return ResponseEntity containing a list of airports in the specified country
     * with HTTP status 200 (OK)
     */
    @GetMapping("/by-country")
    public ResponseEntity<List<Airport>> getAirportsByCountry(@RequestParam String country) {
        List<Airport> airports = airportService.getAirportsByCountry(country);
        return ResponseEntity.ok(airports);
    }

    /**
     * Retrieves an airport by its unique code.
     *
     * @param code the IATA/ICAO code of the airport
     * @return ResponseEntity containing the airport if found (HTTP status 200),
     * or HTTP status 404 if not found
     */
    @GetMapping("/{code}")
    public ResponseEntity<Airport> getAirportByCode(@PathVariable String code) {
        return ResponseEntity.ok(airportService.getAirportByCode(code));
    }

    /**
     * Creates a new airport in the system.
     *
     * @param airport the airport entity to create
     * @return ResponseEntity containing the created airport with HTTP status 200 (OK)
     */
    @PostMapping
    public ResponseEntity<Airport> createAirport(@RequestBody Airport airport) {
        return ResponseEntity.ok(airportService.saveAirport(airport));
    }
}
