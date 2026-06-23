package com.airlinebookingsystem.service;

import com.airlinebookingsystem.entity.Airline;
import com.airlinebookingsystem.repository.AirlineRepository;
import com.airlinebookingsystem.exception.DuplicateResourceException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<Airline> getAllAirlines() {
        log.info("Fetching all airlines");
        return airlineRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Airline> getAllActiveAirlines() {
        log.info("Fetching all active airlines");
        return airlineRepository.findActiveAirlines();
    }

    @Transactional(readOnly = true)
    public Airline getAirlineById(@NonNull Long id) {
        log.info("Fetching airline with ID: {}", id);
        return airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", id));
    }

    @Transactional(readOnly = true)
    public Airline getAirlineByCode(String code) {
        log.info("Fetching airline with code: {}", code);
        return airlineRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Airline", code));
    }

    @Transactional(readOnly = true)
    public List<Airline> getAirlinesByCountry(String country) {
        log.info("Fetching airlines in country: {}", country);
        return airlineRepository.findByCountry(country);
    }

    public Airline createAirline(Airline airline) {
        String upperCode = airline.getCode().toUpperCase();
        airline.setCode(upperCode);

        if (airlineRepository.existsByCode(upperCode)) {
            throw new DuplicateResourceException("Airline code", upperCode);
        }

        log.info("Creating new airline with code: {}", upperCode);
        return airlineRepository.save(airline);
    }

    public Airline updateAirline(@NonNull Long id, Airline airlineDetails) {
        log.info("Updating airline with ID: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", id));

        String newUpperCode = airlineDetails.getCode().toUpperCase();

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

    public void deactivateAirline(@NonNull Long id) {
        log.info("Deactivating airline with ID: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", id));

        airline.setActive(false);
        airlineRepository.save(airline);
    }

    public void reactivateAirline(@NonNull Long id) {
        log.info("Reactivating airline with ID: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", id));

        airline.setActive(true);
        airlineRepository.save(airline);
    }

    public void deleteAirline(@NonNull Long id) {
        log.info("Permanently deleting airline with ID: {}", id);

        if (!airlineRepository.existsById(id)) {
            throw new ResourceNotFoundException("Airline", id);
        }

        airlineRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        String upperCode = code.toUpperCase();
        return airlineRepository.existsByCode(upperCode);
    }
}
