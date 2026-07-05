package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.flight.*;
import com.airlinebookingsystem.entity.Airport;
import com.airlinebookingsystem.entity.Airline;
import com.airlinebookingsystem.entity.Booking;
import com.airlinebookingsystem.entity.Flight;
import com.airlinebookingsystem.exception.DuplicateResourceException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.AirlineRepository;
import com.airlinebookingsystem.repository.AirportRepository;
import com.airlinebookingsystem.repository.FlightRepository;
import com.airlinebookingsystem.util.SeatClassUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final AirlineRepository airlineRepository;

    @Transactional(readOnly = true)
    public List<FlightResponse> getAllFlights() {
        log.info("Fetching all flights");
        return flightRepository.findAll().stream()
                .map(this::mapToFlightResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<FlightResponse> getFlightById(@NonNull Long id) {
        log.info("Fetching flight with ID: {}", id);
        return flightRepository.findById(id).map(this::mapToFlightResponse);
    }

    @Transactional(readOnly = true)
    public Optional<FlightResponse> getFlightByNumber(String flightNumber) {
        log.info("Fetching flight with number: {}", flightNumber);
        return flightRepository.findByFlightNumber(flightNumber).map(this::mapToFlightResponse);
    }

    @Transactional(readOnly = true)
    public FlightSearchResult searchFlights(FlightSearchRequest request) {
        log.info("Searching flights from {} to {} on {}", request.departureAirport(), request.arrivalAirport(), request.departureDate());

        List<FlightSearchResponse> outboundFlights = searchFlightsOneWay(
                request.departureAirport(), request.arrivalAirport(),
                request.departureDate(), request.passengers(),
                request.seatClass(), request.directFlightsOnly());

        List<FlightSearchResponse> returnFlights = null;
        boolean isRoundTrip = request.returnDate() != null;

        if (isRoundTrip) {
            returnFlights = searchFlightsOneWay(
                    request.arrivalAirport(), request.departureAirport(),
                    request.returnDate(), request.passengers(),
                    request.seatClass(), request.directFlightsOnly());
        }

        return new FlightSearchResult(outboundFlights, returnFlights, isRoundTrip);
    }

    @Transactional(readOnly = true)
    public List<FlightSearchResponse> getUpcomingFlights() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Fetching upcoming flights after: {}", now);
        return flightRepository.findUpcomingFlights(now).stream()
                .map(flight -> mapToFlightSearchResponse(flight, "ALL"))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FlightSearchResponse> getFlightsByAirlineCode(String airlineCode) {
        String normalizedCode = airlineCode.toUpperCase();
        log.info("Fetching flights for airline code: {}", normalizedCode);
        return flightRepository.findByAirlineCode(normalizedCode).stream()
                .map(flight -> mapToFlightSearchResponse(flight, "ALL"))
                .collect(Collectors.toList());
    }

    public FlightResponse createFlight(FlightRequest request) {
        // Validate no duplicate flight number
        if (flightRepository.findByFlightNumber(request.flightNumber()).isPresent()) {
            throw new DuplicateResourceException("Flight number", request.flightNumber());
        }

        Airline airline = airlineRepository.findByCode(request.airlineCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Airline", request.airlineCode()));

        Airport departure = airportRepository.findByCode(request.departureAirportCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Airport", request.departureAirportCode()));

        Airport arrival = airportRepository.findByCode(request.arrivalAirportCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Airport", request.arrivalAirportCode()));

        if (departure.getId().equals(arrival.getId())) {
            throw new IllegalArgumentException("Departure and arrival airports cannot be the same");
        }

        int totalSeats = request.economySeats() + request.businessSeats() + request.firstClassSeats();
        int durationMinutes = (int) ChronoUnit.MINUTES.between(request.departureTime(), request.arrivalTime());

        Flight flight = Flight.builder()
                .flightNumber(request.flightNumber().toUpperCase())
                .airline(airline)
                .departureAirport(departure)
                .arrivalAirport(arrival)
                .departureTime(request.departureTime())
                .arrivalTime(request.arrivalTime())
                .duration(durationMinutes)
                .basePrice(request.economyPrice())
                .totalSeats(totalSeats)
                .availableSeats(totalSeats)
                .economySeats(request.economySeats())
                .businessSeats(request.businessSeats())
                .firstClassSeats(request.firstClassSeats())
                .economyPrice(request.economyPrice())
                .businessPrice(request.businessPrice() != null ? request.businessPrice() : BigDecimal.ZERO)
                .firstClassPrice(request.firstClassPrice() != null ? request.firstClassPrice() : BigDecimal.ZERO)
                .aircraft(request.aircraft())
                .status(request.status() != null ? request.status() : Flight.FlightStatus.SCHEDULED)
                .active(true)
                .build();

        log.info("Created flight: {} from {} to {}", flight.getFlightNumber(),
                departure.getCode(), arrival.getCode());

        return mapToFlightResponse(flightRepository.save(flight));
    }

    public FlightResponse updateFlight(@NonNull Long id, FlightRequest request) {
        log.info("Updating flight with ID: {}", id);

        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", id));

        Airport departure = airportRepository.findByCode(request.departureAirportCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Airport", request.departureAirportCode()));

        Airport arrival = airportRepository.findByCode(request.arrivalAirportCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Airport", request.arrivalAirportCode()));

        int durationMinutes = (int) ChronoUnit.MINUTES.between(request.departureTime(), request.arrivalTime());

        // Calculate seat difference to adjust availableSeats proportionally
        int oldTotalSeats = flight.getTotalSeats();
        int newEconomySeats = request.economySeats() != null ? request.economySeats() : flight.getEconomySeats();
        int newBusinessSeats = request.businessSeats() != null ? request.businessSeats() : flight.getBusinessSeats();
        int newFirstClassSeats = request.firstClassSeats() != null ? request.firstClassSeats() : flight.getFirstClassSeats();
        int newTotalSeats = newEconomySeats + newBusinessSeats + newFirstClassSeats;

        // Adjust availableSeats by the same delta as totalSeats
        int seatDelta = newTotalSeats - oldTotalSeats;
        int newAvailableSeats = Math.max(0, flight.getAvailableSeats() + seatDelta);

        flight.setDepartureAirport(departure);
        flight.setArrivalAirport(arrival);
        flight.setDepartureTime(request.departureTime());
        flight.setArrivalTime(request.arrivalTime());
        flight.setDuration(durationMinutes);
        flight.setEconomySeats(newEconomySeats);
        flight.setBusinessSeats(newBusinessSeats);
        flight.setFirstClassSeats(newFirstClassSeats);
        flight.setTotalSeats(newTotalSeats);
        flight.setAvailableSeats(newAvailableSeats);
        flight.setEconomyPrice(request.economyPrice());
        flight.setBusinessPrice(request.businessPrice() != null ? request.businessPrice() : BigDecimal.ZERO);
        flight.setFirstClassPrice(request.firstClassPrice() != null ? request.firstClassPrice() : BigDecimal.ZERO);
        flight.setBasePrice(request.economyPrice());
        flight.setAircraft(request.aircraft());
        if (request.status() != null) flight.setStatus(request.status());

        log.info("Updated flight: {}", flight.getFlightNumber());
        return mapToFlightResponse(flightRepository.save(flight));
    }

    public void deleteFlight(@NonNull Long id) {
        log.info("Deleting flight with ID: {}", id);
        if (!flightRepository.existsById(id)) {
            throw new ResourceNotFoundException("Flight", id);
        }
        flightRepository.deleteById(id);
        log.info("Deleted flight with ID: {}", id);
    }

    // ---- Private helpers -----

    private List<FlightSearchResponse> searchFlightsOneWay(
            String departureCode, String arrivalCode,
            LocalDate departureDate, Integer passengers,
            String seatClass, Boolean directFlightsOnly) {

        Airport departure = airportRepository.findByCode(departureCode)
                .orElseThrow(() -> new ResourceNotFoundException("Departure airport", departureCode));
        Airport arrival = airportRepository.findByCode(arrivalCode)
                .orElseThrow(() -> new ResourceNotFoundException("Arrival airport", arrivalCode));

        LocalDateTime departureDateTime = departureDate.atStartOfDay();
        List<Flight> flights = flightRepository.findAvailableFlights(departure, arrival, departureDateTime);

        int passengerCount = passengers != null ? passengers : 1;
        String seatClassFinal = seatClass != null ? seatClass : "ECONOMY";

        return flights.stream()
                .filter(f -> hasEnoughSeats(f, passengerCount, seatClassFinal))
                .filter(f -> directFlightsOnly == null || !directFlightsOnly || true)
                .map(f -> mapToFlightSearchResponse(f, seatClassFinal))
                .collect(Collectors.toList());
    }

    private boolean hasEnoughSeats(Flight flight, int passengers, String seatClass) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        return SeatClassUtils.hasEnoughSeats(flight, seatClassEnum, passengers);
    }

    private BigDecimal getPriceForSeatClass(Flight flight, String seatClass) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        return SeatClassUtils.getPriceForSeatClass(flight, seatClassEnum);
    }

    private Integer getAvailableSeatsForClass(Flight flight, String seatClass) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        return SeatClassUtils.getAvailableSeatsForClass(flight, seatClassEnum);
    }

    private FlightResponse mapToFlightResponse(Flight flight) {
        return new FlightResponse(
                flight.getId(),
                flight.getFlightNumber(),
                flight.getAirline().getCode(),
                flight.getAirline().getName(),
                flight.getDepartureAirport().getCode(),
                flight.getDepartureAirport().getCity(),
                flight.getArrivalAirport().getCode(),
                flight.getArrivalAirport().getCity(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getDuration(),
                flight.getTotalSeats(),
                flight.getAvailableSeats(),
                flight.getEconomySeats(),
                flight.getBusinessSeats(),
                flight.getFirstClassSeats(),
                flight.getBasePrice(),
                flight.getEconomyPrice(),
                flight.getBusinessPrice(),
                flight.getFirstClassPrice(),
                flight.getStatus(),
                flight.getActive(),
                flight.getAircraft(),
                flight.getCreatedAt(),
                flight.getUpdatedAt()
        );
    }

    private FlightSearchResponse mapToFlightSearchResponse(Flight flight, String seatClass) {
        // For search results: show seats available for the requested class
        // For upcoming/browse: show total available seats across all classes
        boolean isSpecificClass = seatClass != null && !seatClass.equalsIgnoreCase("ALL");
        Integer availableSeats = isSpecificClass
                ? getAvailableSeatsForClass(flight, seatClass)
                : flight.getAvailableSeats();

        return new FlightSearchResponse(
                flight.getId(),
                flight.getFlightNumber(),
                flight.getAirline().getName(),
                flight.getAirline().getCode(),
                flight.getDepartureAirport().getCode(),
                flight.getArrivalAirport().getCode(),
                flight.getDepartureAirport().getCity(),
                flight.getArrivalAirport().getCity(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getDuration(),
                getPriceForSeatClass(flight, seatClass),
                availableSeats,
                flight.getAircraft()
        );
    }
}
