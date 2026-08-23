package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.common.PagedResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    public Optional<FlightResponse> getFlightById(@NonNull Long id) {
        log.info("Fetching flight with ID: {}", id);
        return flightRepository.findById(id).map(this::mapToFlightResponse);
    }

    /**
     * Looks up a flight number. The same service runs on many dates, so this
     * resolves to the next upcoming instance, falling back to the most recent
     * past one when the service is no longer scheduled.
     */
    @Transactional(readOnly = true)
    public Optional<FlightResponse> getFlightByNumber(String flightNumber) {
        log.info("Fetching flight with number: {}", flightNumber);

        List<Flight> instances =
                flightRepository.findByFlightNumberOrderByDepartureTimeAsc(flightNumber);
        if (instances.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return instances.stream()
                .filter(f -> f.getDepartureTime().isAfter(now))
                .findFirst()
                .or(() -> Optional.of(instances.get(instances.size() - 1)))
                .map(this::mapToFlightResponse);
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

    /**
     * How far ahead the timetable currently runs.
     *
     * <p>The rolling schedule keeps a moving window of dates, so a search
     * beyond it finds nothing and always will — which is worth telling a
     * caller apart from "this route is full that day".
     *
     * @return the last date with a bookable flight, or empty if there are none
     */
    @Transactional(readOnly = true)
    public Optional<LocalDate> getLatestDepartureDate() {
        return Optional.ofNullable(flightRepository.findLatestDepartureTime())
                .map(LocalDateTime::toLocalDate);
    }

    @Transactional(readOnly = true)
    public List<FlightSearchResponse> getUpcomingFlights() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
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

    /**
     * Upcoming flights arriving at an airport, from anywhere in the network.
     * Used by the destination view, where the traveller has picked a place to
     * go but not yet an origin.
     */
    @Transactional(readOnly = true)
    public List<FlightSearchResponse> getUpcomingArrivals(String airportCode) {
        String normalizedCode = airportCode.toUpperCase();
        log.info("Fetching upcoming arrivals at: {}", normalizedCode);

        return flightRepository.findUpcomingArrivals(normalizedCode, LocalDateTime.now(ZoneOffset.UTC))
                .stream()
                .map(flight -> mapToFlightSearchResponse(flight, "ALL"))
                .collect(Collectors.toList());
    }

    /**
     * Searches each leg of a multi-city itinerary independently, preserving the
     * order the legs were submitted in. Each leg reuses the same one-way search
     * as a simple search, so availability rules stay consistent.
     */
    @Transactional(readOnly = true)
    public MultiCitySearchResult searchMultiCity(MultiCitySearchRequest request) {
        log.info("Multi-city search across {} legs", request.legs().size());

        List<MultiCitySearchResult.LegResult> results = new ArrayList<>();
        int legNumber = 1;

        for (MultiCitySearchRequest.Leg leg : request.legs()) {
            List<FlightSearchResponse> flights = searchFlightsOneWay(
                    leg.departureAirport(), leg.arrivalAirport(),
                    leg.departureDate(), request.passengers(),
                    request.seatClass(), request.directFlightsOnly());

            results.add(new MultiCitySearchResult.LegResult(
                    legNumber++,
                    leg.departureAirport().toUpperCase(),
                    leg.arrivalAirport().toUpperCase(),
                    leg.departureDate(),
                    flights));
        }

        return new MultiCitySearchResult(results);
    }

    /**
     * Live status for a single flight, derived from its timetable at request time.
     */
    @Transactional(readOnly = true)
    public Optional<FlightStatusResponse> getFlightStatus(@NonNull Long id) {
        log.info("Fetching live status for flight ID: {}", id);
        return flightRepository.findById(id).map(this::mapToFlightStatusResponse);
    }

    public FlightResponse createFlight(FlightRequest request) {
        // A flight number may repeat across dates, so only the same number at
        // the same departure time counts as a duplicate.
        if (flightRepository.existsByFlightNumberAndDepartureTime(
                request.flightNumber(), request.departureTime())) {
            throw new DuplicateResourceException(
                    "Flight number", request.flightNumber() + " at " + request.departureTime());
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

    /** How long before departure a flight is considered to be boarding. */
    private static final int BOARDING_WINDOW_MINUTES = 45;

    private FlightStatusResponse mapToFlightStatusResponse(Flight flight) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String status = deriveStatus(flight, now);
        int progress = "CANCELLED".equals(status) ? 0 : deriveProgress(flight, now);

        return new FlightStatusResponse(
                flight.getId(),
                flight.getFlightNumber(),
                flight.getAirline().getName(),
                flight.getDepartureAirport().getCode(),
                flight.getDepartureAirport().getCity(),
                flight.getDepartureAirport().getLatitude(),
                flight.getDepartureAirport().getLongitude(),
                flight.getArrivalAirport().getCode(),
                flight.getArrivalAirport().getCity(),
                flight.getArrivalAirport().getLatitude(),
                flight.getArrivalAirport().getLongitude(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getDepartureAirport().getTimezone(),
                flight.getArrivalAirport().getTimezone(),
                flight.getDuration(),
                status,
                progress,
                flight.getGate(),
                flight.getTerminal(),
                flight.getAircraft()
        );
    }

    /**
     * Derives the live status from the timetable. A persisted CANCELLED/DELAYED
     * state always wins, since those are airline decisions rather than a
     * function of the clock.
     */
    private String deriveStatus(Flight flight, LocalDateTime now) {
        if (flight.getStatus() == Flight.FlightStatus.CANCELLED) return "CANCELLED";
        if (flight.getStatus() == Flight.FlightStatus.DELAYED) return "DELAYED";

        LocalDateTime departure = flight.getDepartureTime();
        LocalDateTime arrival = flight.getArrivalTime();

        if (now.isBefore(departure.minusMinutes(BOARDING_WINDOW_MINUTES))) return "SCHEDULED";
        if (now.isBefore(departure)) return "BOARDING";
        if (now.isBefore(arrival)) return "IN_AIR";
        return "LANDED";
    }

    /** 0 before departure, 100 after arrival, linear in between. */
    private int deriveProgress(Flight flight, LocalDateTime now) {
        LocalDateTime departure = flight.getDepartureTime();
        LocalDateTime arrival = flight.getArrivalTime();

        if (!now.isAfter(departure)) return 0;
        if (!now.isBefore(arrival)) return 100;

        long totalSeconds = ChronoUnit.SECONDS.between(departure, arrival);
        if (totalSeconds <= 0) return 100;

        long elapsedSeconds = ChronoUnit.SECONDS.between(departure, now);
        return (int) Math.round((elapsedSeconds * 100.0) / totalSeconds);
    }

    /** Largest page the API will serve, so a bad `size` cannot ask for everything. */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * A page of flights for the admin listing, newest departures first.
     *
     * <p>The alternative — returning all of them and slicing in the browser —
     * moves several thousand rows over the wire to render forty, and gets worse
     * every time the timetable grows.
     */
    @Transactional(readOnly = true)
    public PagedResponse<FlightResponse> searchFlightsPaged(String search, int page, int size) {
        String term = search == null ? "" : search.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(
                safePage, safeSize, Sort.by(Sort.Direction.DESC, "departureTime"));

        log.info("Paged flight search — term '{}', page {}, size {}", term, safePage, safeSize);
        return PagedResponse.from(
                flightRepository.searchPaged(term, pageable), this::mapToFlightResponse);
    }

    /**
     * @param seatClass a cabin name, or {@code "ALL"} for browse views where the
     *                  caller has not chosen one.
     */
    private FlightSearchResponse mapToFlightSearchResponse(Flight flight, String seatClass) {
        boolean isSpecificClass = seatClass != null && !seatClass.equalsIgnoreCase("ALL");

        // A chosen cabin: its own seats. Browsing: every seat on the aircraft.
        Integer availableSeats = isSpecificClass
                ? getAvailableSeatsForClass(flight, seatClass)
                : flight.getAvailableSeats();

        // A chosen cabin: its own fare. Browsing: the cheapest cabin, as a lead-in.
        BigDecimal price = isSpecificClass
                ? getPriceForSeatClass(flight, seatClass)
                : SeatClassUtils.getPriceForSeatClass(flight, Booking.SeatClass.ECONOMY);

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
                flight.getDepartureAirport().getTimezone(),
                flight.getArrivalAirport().getTimezone(),
                flight.getDuration(),
                price,
                availableSeats,
                flight.getAircraft()
        );
    }
}
