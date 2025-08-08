package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.FlightSearchRequest;
import com.airlinebookingsystem.dto.FlightSearchResponse;
import com.airlinebookingsystem.dto.FlightSearchResult;
import com.airlinebookingsystem.entity.Airport;
import com.airlinebookingsystem.entity.Airline;
import com.airlinebookingsystem.entity.Booking;
import com.airlinebookingsystem.entity.Flight;
import com.airlinebookingsystem.repository.AirlineRepository;
import com.airlinebookingsystem.repository.AirportRepository;
import com.airlinebookingsystem.repository.FlightRepository;
import com.airlinebookingsystem.util.SeatClassUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing flight operations in the airline booking system.
 * Handles flight-related business logic including searching, creating, updating, and deleting flights.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FlightService {
    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final AirlineRepository airlineRepository;

    /**
     * Retrieves all flights from the system.
     *
     * @return a list of all flights
     */
    public List<Flight> getAllFlights() {
        log.info("Fetching all flights");
        return flightRepository.findAll();
    }

    /**
     * Retrieves a flight by its ID.
     *
     * @param id the ID of the flight to retrieve
     * @return an Optional containing the flight if found, or empty if not found
     */
    public Optional<Flight> getFlightById(Long id) {
        return flightRepository.findById(id);
    }

    /**
     * Retrieves a flight by its flight number.
     *
     * @param flightNumber the flight number to search for
     * @return an Optional containing the flight if found, or empty if not found
     */
    public Optional<Flight> getFlightByNumber(String flightNumber) {
        log.info("Fetching flight with number: {}", flightNumber);
        return flightRepository.findByFlightNumber(flightNumber);
    }

    /**
     * Searches for available flights based on search criteria.
     * Supports both one-way and round-trip searches.
     *
     * @param request the search criteria including departure airport, arrival airport, and date
     * @return FlightSearchResult containing outbound flights and optional return flights
     * @throws RuntimeException if departure or arrival airport is not found
     */
    public FlightSearchResult searchFlights(FlightSearchRequest request) {
        log.info("Searching flights from {} to {} on {} for {} passengers in {} class",
                request.departureAirport(),
                request.arrivalAirport(),
                request.departureDate(),
                request.passengers() != null ? request.passengers() : 1,
                request.seatClass() != null ? request.seatClass() : "ECONOMY");

        // Search outbound flights
        List<FlightSearchResponse> outboundFlights = searchFlightsOneWay(
                request.departureAirport(),
                request.arrivalAirport(),
                request.departureDate(),
                request.passengers(),
                request.seatClass(),
                request.directFlightsOnly()
        );

        List<FlightSearchResponse> returnFlights = null;
        boolean isRoundTrip = request.returnDate() != null;

        // Search return flights if a return date is provided
        if (isRoundTrip) {
            log.info("Searching return flights from {} to {} on {}",
                    request.arrivalAirport(),
                    request.departureAirport(),
                    request.returnDate());

            returnFlights = searchFlightsOneWay(
                    request.arrivalAirport(),      // Swap departure/arrival for return
                    request.departureAirport(),
                    request.returnDate(),
                    request.passengers(),
                    request.seatClass(),
                    request.directFlightsOnly()
            );
        }
        log.info("Found {} outbound flights{}", outboundFlights.size(),
                isRoundTrip ? " and " + (returnFlights != null ? returnFlights.size() : 0) + " return flights" : "");

        return new FlightSearchResult(outboundFlights, returnFlights, isRoundTrip);
    }

    /**
     * Searches for one-way flights with the specified criteria.
     *
     * @param departureCode the departure airport code
     * @param arrivalCode the arrival airport code
     * @param departureDate the departure date
     * @param passengers number of passengers (nullable)
     * @param seatClass seat class preference (nullable)
     * @param directFlightsOnly whether to show only direct flights (nullable)
     * @return list of available flights matching the criteria
     */
    private List<FlightSearchResponse> searchFlightsOneWay(
            String departureCode,
            String arrivalCode,
            LocalDate departureDate,
            Integer passengers,
            String seatClass,
            Boolean directFlightsOnly) {

        Airport departure = airportRepository.findByCode(departureCode)
                .orElseThrow(() -> new RuntimeException("Departure airport not found: " + departureCode));

        Airport arrival = airportRepository.findByCode(arrivalCode)
                .orElseThrow(() -> new RuntimeException("Arrival airport not found: " + arrivalCode));

        LocalDateTime departureDateTime = departureDate.atStartOfDay();

        List<Flight> flights = flightRepository.findAvailableFlights(
                departure, arrival, departureDateTime);

        int passengerCount = passengers != null ? passengers : 1;
        String seatClassFinal = seatClass != null ? seatClass : "ECONOMY";
        boolean directOnly = directFlightsOnly != null ? directFlightsOnly : false;

        return flights.stream()
                .filter(flight -> hasEnoughSeats(flight, passengerCount, seatClassFinal))
                .filter(flight -> !directOnly || isDirect(flight))
                .map(flight -> mapToFlightSearchResponse(flight, seatClassFinal))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a flight has enough available seats for the requested number of passengers
     * in the specified seat class.
     *
     * @param flight the flight to check
     * @param passengers number of passengers
     * @param seatClass the requested seat class
     * @return true if enough seats are available, false otherwise
     */
    private boolean hasEnoughSeats(Flight flight, int passengers, String seatClass) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        return SeatClassUtils.hasEnoughSeats(flight, seatClassEnum, passengers);
    }

    /**
     * Checks if a flight is a direct flight.
     * Currently, assumes all flights are direct.
     *
     * @param flight the flight to check
     * @return true if the flight is direct, false otherwise
     */
    private boolean isDirect(Flight flight) {
        // ToDo Implement direct flight logic
        // This could check for connecting flights, stops, etc.
        // For now, assuming all flights are direct
        return true;
    }

    /**
     * Gets the price for a specific seat class, with fallback logic.
     *
     * @param flight the flight
     * @param seatClass the seat class
     * @return the price for the specified seat class
     */
    private BigDecimal getPriceForSeatClass(Flight flight, String seatClass) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        return SeatClassUtils.getPriceForSeatClass(flight, seatClassEnum);
    }

    /**
     * Gets the number of available seats for a specific class.
     *
     * @param flight the flight
     * @param seatClass the seat class
     * @return the number of available seats in the specified class
     */
    private Integer getAvailableSeatsForClass(Flight flight, String seatClass) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        return SeatClassUtils.getAvailableSeatsForClass(flight, seatClassEnum);
    }

    /**
     * Creates a new flight in the system.
     *
     * @param flight the flight to create
     * @return the created flight with assigned ID
     * @throws RuntimeException if flight data is invalid
     */
    public Flight createFlight(Flight flight) {
        // Check for duplicate flight numbers
        if (flight.getFlightNumber() != null &&
                flightRepository.findByFlightNumber(flight.getFlightNumber()).isPresent()) {
            throw new RuntimeException("Flight number already exists: " + flight.getFlightNumber());
        }

        // Validate and fetch the airline
        if (flight.getAirline() != null && flight.getAirline().getId() != null) {
            Airline airline = airlineRepository.findById(flight.getAirline().getId())
                    .orElseThrow(() -> new RuntimeException("Airline not found with ID: " + flight.getAirline().getId()));
            flight.setAirline(airline);
        } else {
            throw new RuntimeException("Airline is required");
        }

        // Validate and fetch departure airport
        if (flight.getDepartureAirport() != null && flight.getDepartureAirport().getId() != null) {
            Airport departureAirport = airportRepository.findById(flight.getDepartureAirport().getId())
                    .orElseThrow(() -> new RuntimeException("Departure airport not found with ID: " + flight.getDepartureAirport().getId()));
            flight.setDepartureAirport(departureAirport);
        } else {
            throw new RuntimeException("Departure airport is required");
        }

        // Validate and fetch arrival airport
        if (flight.getArrivalAirport() != null && flight.getArrivalAirport().getId() != null) {
            Airport arrivalAirport = airportRepository.findById(flight.getArrivalAirport().getId())
                    .orElseThrow(() -> new RuntimeException("Arrival airport not found with ID: " + flight.getArrivalAirport().getId()));
            flight.setArrivalAirport(arrivalAirport);
        } else {
            throw new RuntimeException("Arrival airport is required");
        }

        // Additional validation
        if (flight.getDepartureAirport().getId().equals(flight.getArrivalAirport().getId())) {
            throw new RuntimeException("Departure and arrival airports cannot be the same");
        }
        log.info("Successfully created new flight: {} from {} to {}",
                flight.getFlightNumber(),
                flight.getDepartureAirport().getCode(),
                flight.getArrivalAirport().getCode());

        return flightRepository.save(flight);
    }

    /**
     * Updates an existing flight's details.
     *
     * @param id            the ID of the flight to update
     * @param flightDetails the new flight details
     * @return the updated flight
     * @throws RuntimeException if the flight is not found
     */
    public Flight updateFlight(Long id, Flight flightDetails) {
        log.info("Updating flight with ID: {}", id);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        flight.setDepartureTime(flightDetails.getDepartureTime());
        flight.setArrivalTime(flightDetails.getArrivalTime());
        flight.setBasePrice(flightDetails.getBasePrice());
        flight.setAvailableSeats(flightDetails.getAvailableSeats());
        flight.setStatus(flightDetails.getStatus());

        log.info("Successfully updated flight with ID: {}", id);
        return flightRepository.save(flight);
    }

    /**
     * Deletes a flight from the system.
     *
     * @param id the ID of the flight to delete
     * @throws RuntimeException if flight is not found
     */
    public void deleteFlight(Long id) {
        log.info("Deleting flight with ID: {}", id);
        // Check if a flight exists before deletion
        if (!flightRepository.existsById(id)) {
            throw new RuntimeException("Flight not found with ID: " + id);
        }
        flightRepository.deleteById(id);
        log.info("Successfully deleted flight with ID: {}", id);
    }

    /**
     * Retrieves all upcoming flights from the current date and time.
     *
     * @return a list of upcoming flights
     */
    public List<FlightSearchResponse> getUpcomingFlights() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Fetching upcoming flights after: {}", now);
        return flightRepository.findUpcomingFlights(now).stream()
                .map(flight -> mapToFlightSearchResponse(flight, "ECONOMY"))
                .collect(Collectors.toList());

    }

    /**
     * Retrieves all flights operated by a specific airline using airline code.
     *
     * @param airlineCode the code of the airline (e.g., "AA", "DL", "UA")
     * @return a list of flights operated by the specified airline
     * @throws RuntimeException if the airline is not found
     */
    public List<FlightSearchResponse> getFlightsByAirlineCode(String airlineCode) {

        String normalizedCode = airlineCode.toUpperCase();
        log.info("Fetching flights for airline code: {}", normalizedCode);

        List<Flight> flights = flightRepository.findByAirlineCode(normalizedCode);

        List<FlightSearchResponse> results = flights.stream()
                .map(flight -> mapToFlightSearchResponse(flight, "ECONOMY"))
                .collect(Collectors.toList());

        log.info("Found {} flights for airline code: {}", results.size(), normalizedCode);
        return results;
    }

    /**
     * Maps a Flight entity to a FlightSearchResponse DTO.
     *
     * @param flight the flight entity to map
     * @return the mapped FlightSearchResponse
     */
    private FlightSearchResponse mapToFlightSearchResponse(Flight flight, String seatClass) {
        BigDecimal price = getPriceForSeatClass(flight, seatClass);
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
                price,
                getAvailableSeatsForClass(flight, seatClass),
                flight.getAircraft()
        );
    }
}
