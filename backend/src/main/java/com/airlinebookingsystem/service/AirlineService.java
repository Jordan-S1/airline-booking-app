package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.airline.AirlineRequest;
import com.airlinebookingsystem.dto.airline.AirlineResponse;
import com.airlinebookingsystem.entity.Airline;
import com.airlinebookingsystem.exception.DuplicateResourceException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.AirlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AirlineService {

    private final AirlineRepository airlineRepository;

    @Transactional(readOnly = true)
    public List<AirlineResponse> getAllAirlines() {
        log.info("Fetching all airlines");
        return airlineRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AirlineResponse> getAllActiveAirlines() {
        log.info("Fetching all active airlines");
        return airlineRepository.findActiveAirlines().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<AirlineResponse> getAirlineById(@NonNull Long id) {
        log.info("Fetching airline with ID: {}", id);
        return airlineRepository.findById(id).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Optional<AirlineResponse> getAirlineByCode(String code) {
        log.info("Fetching airline with code: {}", code);
        return airlineRepository.findByCode(code.toUpperCase()).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<AirlineResponse> getAirlinesByCountry(String country) {
        log.info("Fetching airlines in country: {}", country);
        return airlineRepository.findByCountry(country).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AirlineResponse createAirline(AirlineRequest request) {
        String upperCode = request.code().toUpperCase();

        if (airlineRepository.existsByCode(upperCode)) {
            throw new DuplicateResourceException("Airline code", upperCode);
        }

        Airline airline = Airline.builder()
                .code(upperCode)
                .name(request.name())
                .logoUrl(request.logoUrl())
                .website(request.website())
                .country(request.country())
                .active(request.active() != null ? request.active() : true)
                .build();

        log.info("Creating new airline with code: {}", upperCode);
        return mapToResponse(airlineRepository.save(airline));
    }

    public AirlineResponse updateAirline(@NonNull Long id, AirlineRequest request) {
        log.info("Updating airline with ID: {}", id);

        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline", id));

        String newUpperCode = request.code().toUpperCase();

        if (!airline.getCode().equals(newUpperCode) && airlineRepository.existsByCode(newUpperCode)) {
            throw new DuplicateResourceException("Airline code", newUpperCode);
        }

        airline.setCode(newUpperCode);
        airline.setName(request.name());
        airline.setLogoUrl(request.logoUrl());
        airline.setWebsite(request.website());
        airline.setCountry(request.country());
        if (request.active() != null) airline.setActive(request.active());

        return mapToResponse(airlineRepository.save(airline));
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
        return airlineRepository.existsByCode(code.toUpperCase());
    }

    private AirlineResponse mapToResponse(Airline airline) {
        return new AirlineResponse(
                airline.getId(),
                airline.getCode(),
                airline.getName(),
                airline.getLogoUrl(),
                airline.getWebsite(),
                airline.getCountry(),
                airline.getActive(),
                airline.getCreatedAt(),
                airline.getUpdatedAt()
        );
    }
}
