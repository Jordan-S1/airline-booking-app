package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.airport.AirportRequest;
import com.airlinebookingsystem.dto.airport.AirportResponse;
import com.airlinebookingsystem.entity.Airport;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.AirportRepository;
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
public class AirportService {

    private final AirportRepository airportRepository;

    @Transactional(readOnly = true)
    public List<AirportResponse> getAllAirports() {
        log.info("Fetching all airports");
        return airportRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<AirportResponse> getAirportByCode(String code) {
        log.info("Fetching airport with code: {}", code);
        return airportRepository.findByCode(code.toUpperCase()).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<AirportResponse> searchAirports(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllAirports();
        }
        return airportRepository.findBySearchQuery(query.trim()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AirportResponse> getAirportsByCountry(String country) {
        log.info("Fetching airports for country: {}", country);
        return airportRepository.findByCountryOrderByCity(country).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AirportResponse saveAirport(AirportRequest request) {
        Airport airport = Airport.builder()
                .code(request.code().toUpperCase())
                .name(request.name())
                .city(request.city())
                .country(request.country())
                .countryCode(normaliseCountryCode(request.countryCode()))
                .timezone(request.timezone())
                .build();

        log.info("Saving airport with code: {}", airport.getCode());
        return mapToResponse(airportRepository.save(airport));
    }

    public AirportResponse updateAirport(@NonNull Long id, AirportRequest request) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport", id));

        // Code changes are not allowed — IATA codes are permanent identifiers
        // If the code needs to change, delete and recreate the airport
        airport.setName(request.name());
        airport.setCity(request.city());
        airport.setCountry(request.country());
        if (request.countryCode() != null) airport.setCountryCode(normaliseCountryCode(request.countryCode()));
        if (request.timezone() != null) airport.setTimezone(request.timezone());

        log.info("Updated airport: {}", airport.getCode());
        return mapToResponse(airportRepository.save(airport));
    }

    public void deleteAirport(@NonNull Long id) {
        if (!airportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Airport", id);
        }
        airportRepository.deleteById(id);
    }

    /**
     * The country_code column is constrained to uppercase alpha-2, so normalise
     * here rather than rejecting a lowercase code the client meant correctly.
     */
    private String normaliseCountryCode(String countryCode) {
        return countryCode == null ? null : countryCode.toUpperCase();
    }

    private AirportResponse mapToResponse(Airport airport) {
        return new AirportResponse(
                airport.getId(),
                airport.getCode(),
                airport.getName(),
                airport.getCity(),
                airport.getCountry(),
                airport.getCountryCode(),
                airport.getTimezone(),
                airport.getLatitude(),
                airport.getLongitude(),
                airport.getCreatedAt(),
                airport.getUpdatedAt()
        );
    }
}
