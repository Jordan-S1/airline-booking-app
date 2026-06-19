package com.airlinebookingsystem.service;

import com.airlinebookingsystem.entity.Airline;
import com.airlinebookingsystem.repository.AirlineRepository;
import com.airlinebookingsystem.exception.DuplicateResourceException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing airline operations in the airline booking system.
 * Handles airline-related business logic including CRUD operations and business
 * rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AirlineService {
    private final AirlineRepository airlineRepository;

    /**
     * Retrieves all airlines from the system.
     *
     * @return a list of all airlines
     */
    public List<Airline> getAllAirlines() {
        log.info("Fetching all airlines");
        return airlineRepository.findAll();
    }

    /**
     * Retrieves all active airlines from the system.
     *
     * @return a list of all active airlines
     */
    public List<Airline> getAllActiveAirlines() {
        log.info("Fetching all active airlines");
        return airlineRepository.findActiveAirlines();
    }

    /**
     * Retrieves an airline by its ID.
     *
     * @param id the ID of the airline to retrieve
     * @return an Optional containing the airline if found, or empty if not found
     */
    public Optional<Airline> getAirlineById(Long id) {
        log.info("Fetching airline with ID: {}", id);
        return airlineRepository.findById(id);
    }

    /**
     * Retrieves an airline by its code.
     *
     * @param code the airline code to search for
     * @return an Optional containing the airline if found, or empty if not found
     */
    public Optional<Airline> getAirlineByCode(String code) {
        log.info("Fetching airline with code: {}", code);
        return airlineRepository.findByCode(code.toUpperCase());
    }

    /**
     * Retrieves airlines by country.
     *
     * @param country the country to search for airlines in
     * @return a list of active airlines in the specified country
     */
    public List<Airline> getAirlinesByCountry(String country) {
        log.info("Fetching airlines in country: {}", country);
        return airlineRepository.findByCountry(country);
    }

    /**
     * Creates a new airline in the system.
     *
     * @param airline the airline to create
     * @return the created airline with assigned ID
     * @throws DuplicateResourceException if an airline with the same code already
     *                                    exists
     */
    public Airline createAirline(Airline airline) {
        String upperCode = airline.getCode().toUpperCase();
        airline.setCode(upperCode);

        log.info("Creating new airline with code: {}", upperCode);

        if (airlineRepository.existsByCode(upperCode)) {
            throw new DuplicateResourceException("Airline code", upperCode);
        }

        return airlineRepository.save(airline);
    }

    /**
     * Updates an existing airline's details.
     *
     * @param id             the ID of the airline to update
     * @param airlineDetails the new airline details
     * @return the updated airline
     * @throws ResourceNotFoundException  if the airline is not found
     * @throws DuplicateResourceException if the airline code is already in use
     */
    public Airline updateAirline(Long id, Airline airlineDetails) {
        log.info("Updating airline with ID: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", id));

        String newUpperCode = airlineDetails.getCode().toUpperCase();

        // Check if code is being changed and if new code already exists
        if (!airline.getCode().equals(newUpperCode) &&
                airlineRepository.existsByCode(newUpperCode)) {
            throw new DuplicateResourceException("Airline code", newUpperCode);
        }

        airline.setCode(newUpperCode);
        airline.setName(airlineDetails.getName());
        airline.setLogoUrl(airlineDetails.getLogoUrl());
        airline.setWebsite(airlineDetails.getWebsite());
        airline.setCountry(airlineDetails.getCountry());
        airline.setActive(airlineDetails.getActive());

        return airlineRepository.save(airline);
    }

    /**
     * Deactivates an airline instead of deleting it.
     * This is softly deleted to maintain data integrity.
     *
     * @param id the ID of the airline to deactivate
     * @throws ResourceNotFoundException if the airline is not found
     */
    public void deactivateAirline(Long id) {
        log.info("Deactivating airline with ID: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", id));

        airline.setActive(false);
        airlineRepository.save(airline);
    }

    /**
     * Reactivates a previously deactivated airline.
     *
     * @param id the ID of the airline to reactivate
     * @throws ResourceNotFoundException if the airline is not found
     */
    public void reactivateAirline(Long id) {
        log.info("Reactivating airline with ID: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", id));

        airline.setActive(true);
        airlineRepository.save(airline);
    }

    /**
     * Permanently deletes an airline from the system.
     * Use with caution - this will affect flight records.
     *
     * @param id the ID of the airline to delete
     */
    public void deleteAirline(Long id) {
        log.info("Permanently deleting airline with ID: {}", id);
        airlineRepository.deleteById(id);
    }

    /**
     * Checks if an airline exists with the given code.
     * The code is automatically converted to uppercase for consistent checking.
     *
     * @param code the airline code to check
     * @return true if an airline exists with the given code, false otherwise
     */
    public boolean existsByCode(String code) {
        String upperCode = code.toUpperCase();
        return airlineRepository.existsByCode(upperCode);
    }

}
