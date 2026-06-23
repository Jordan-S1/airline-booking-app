package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.entity.Airline;
import com.airlinebookingsystem.service.AirlineService;
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
public class AirlineController {

    private final AirlineService airlineService;

    /**
     * Retrieves all airlines from the system.
     *
     * @return ResponseEntity containing a list of all airlines
     */
    @GetMapping
    public ResponseEntity<List<Airline>> getAllAirlines() {
        log.info("GET /airlines");
        return ResponseEntity.ok(airlineService.getAllAirlines());
    }

    /**
     * Retrieves all active airlines from the system.
     *
     * @return ResponseEntity containing a list of all active airlines
     */
    @GetMapping("/active")
    public ResponseEntity<List<Airline>> getAllActiveAirlines() {
        log.info("GET /airlines/active");
        return ResponseEntity.ok(airlineService.getAllActiveAirlines());
    }

    /**
     * Retrieves an airline by its ID.
     *
     * @param id the ID of the airline to retrieve
     * @return ResponseEntity containing the airline if found, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Airline> getAirlineById(@PathVariable @NonNull Long id) {
        log.info("GET /airlines/{}", id);
        return ResponseEntity.ok(airlineService.getAirlineById(id));
    }

    /**
     * Retrieves an airline by its code.
     *
     * @param code the airline code to search for
     * @return ResponseEntity containing the airline if found, or 404 if not found
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Airline> getAirlineByCode(@PathVariable String code) {
        log.info("GET /airlines/code/{}", code);
        return ResponseEntity.ok(airlineService.getAirlineByCode(code));
    }

    /**
     * Retrieves airlines by country.
     *
     * @param country the country to search for airlines in
     * @return ResponseEntity containing a list of airlines in the specified country
     */
    @GetMapping("/country/{country}")
    public ResponseEntity<List<Airline>> getAirlinesByCountry(@PathVariable String country) {
        log.info("GET /airlines/country/{}", country);
        return ResponseEntity.ok(airlineService.getAirlinesByCountry(country));
    }

    /**
     * Creates a new airline in the system.
     *
     * @param airline the airline to create
     * @return ResponseEntity containing the created airline with status 201
     */
    @PostMapping
    public ResponseEntity<Airline> createAirline(@Valid @RequestBody Airline airline) {
        log.info("POST /airlines - code: {}", airline.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.createAirline(airline));
    }

    /**
     * Updates an existing airline's details.
     *
     * @param id             the ID of the airline to update
     * @param airlineDetails the new airline details
     * @return ResponseEntity containing the updated airline
     */
    @PutMapping("/{id}")
    public ResponseEntity<Airline> updateAirline(@PathVariable @NonNull Long id,
            @Valid @RequestBody Airline airlineDetails) {
        log.info("PUT /airlines/{}", id);
        return ResponseEntity.ok(airlineService.updateAirline(id, airlineDetails));
    }

    /**
     * Deactivates an airline (soft delete).
     *
     * @param id the ID of the airline to deactivate
     * @return ResponseEntity with status 204 if successful
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateAirline(@PathVariable @NonNull Long id) {
        log.info("PATCH /airlines/{}/deactivate", id);
        airlineService.deactivateAirline(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a previously deactivated airline.
     *
     * @param id the ID of the airline to reactivate
     * @return ResponseEntity with status 204 if successful
     */
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateAirline(@PathVariable @NonNull Long id) {
        log.info("PATCH /airlines/{}/reactivate", id);
        airlineService.reactivateAirline(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently deletes an airline from the system.
     * Use with caution - this will affect flight records.
     *
     * @param id the ID of the airline to delete
     * @return ResponseEntity with status 204 if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirline(@PathVariable @NonNull Long id) {
        log.info("DELETE /airlines/{}", id);
        airlineService.deleteAirline(id);
        return ResponseEntity.noContent().build();
    }
}
