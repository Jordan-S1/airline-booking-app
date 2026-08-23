package com.airlinebookingsystem.repository;

import com.airlinebookingsystem.entity.Flight;
import com.airlinebookingsystem.entity.Airport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing Flight entities in the database.
 * Provides CRUD operations and custom query methods for flights.
 */
@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    /**
     * One page of flights matching a free-text term across flight number,
     * either airport code, and airline name.
     *
     * <p>An empty term matches everything, which keeps the unfiltered listing on
     * the same query rather than needing a second one. The joins are spelled out
     * so the derived count query does not have to guess at them, and are fetched
     * eagerly because the response DTO reads the airline and both airports for
     * every row — left implicit, this is a textbook N+1.
     */
    @Query(value = """
            SELECT f FROM Flight f
            JOIN FETCH f.airline al
            JOIN FETCH f.departureAirport dep
            JOIN FETCH f.arrivalAirport arr
            WHERE LOWER(f.flightNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(dep.code)       LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(arr.code)       LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(al.name)        LIKE LOWER(CONCAT('%', :search, '%'))
            """,
            countQuery = """
            SELECT COUNT(f) FROM Flight f
            WHERE LOWER(f.flightNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(f.departureAirport.code) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(f.arrivalAirport.code)   LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(f.airline.name)          LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Flight> searchPaged(@Param("search") String search, Pageable pageable);

    /**
     * Finds every dated instance of a flight number, earliest first.
     *
     * <p>Since V7 a flight number is only unique per departure time — the same
     * service runs daily — so callers wanting "the" flight must choose an
     * instance (normally the next upcoming one).
     *
     * @param flightNumber the flight number to search for
     * @return all instances of that flight number, ordered by departure time
     */
    List<Flight> findByFlightNumberOrderByDepartureTimeAsc(String flightNumber);

    /**
     * Checks whether a specific dated instance already exists, used to reject
     * duplicates when creating a flight.
     */
    boolean existsByFlightNumberAndDepartureTime(String flightNumber, LocalDateTime departureTime);

    /**
     * Finds flights that departed before the cutoff and were never booked.
     * These are safe to delete; booked flights are retained as history.
     */
    @Query("SELECT f FROM Flight f WHERE f.departureTime < :cutoff AND f.bookings IS EMPTY")
    List<Flight> findPrunableFlights(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Finds available flights matching the specified criteria.
     *
     * @param departure     the departure airport
     * @param arrival       the arrival airport
     * @param departureDate the desired departure date and time
     * @return a list of available flights matching the criteria
     */
    @Query("SELECT f FROM Flight f WHERE " +
            "f.departureAirport = :departure AND " +
            "f.arrivalAirport = :arrival AND " +
            "DATE(f.departureTime) = DATE(:departureDate) AND " +
            "f.active = true AND " +
            "f.availableSeats > 0")
    List<Flight> findAvailableFlights(
            @Param("departure") Airport departure,
            @Param("arrival") Airport arrival,
            @Param("departureDate") LocalDateTime departureDate
    );

    /**
     * Searches for flights based on departure and arrival airports and date range.
     *
     * @param departureCode the departure airport code
     * @param arrivalCode   the arrival airport code
     * @param startDate     the start of the date range
     * @param endDate       the end of the date range
     * @return a list of flights matching the search criteria
     */
    @Query("SELECT f FROM Flight f WHERE " +
            "f.departureAirport.code = :departureCode AND " +
            "f.arrivalAirport.code = :arrivalCode AND " +
            "f.departureTime BETWEEN :startDate AND :endDate AND " +
            "f.active = true")
    List<Flight> searchFlights(
            @Param("departureCode") String departureCode,
            @Param("arrivalCode") String arrivalCode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Retrieves all flights operated by a specific airline using airline code.
     *
     * @param airlineCode the code of the airline
     * @return a list of flights operated by the specified airline
     */
    @Query("SELECT f FROM Flight f WHERE f.airline.code = :airlineCode AND f.active = true")
    List<Flight> findByAirlineCode(@Param("airlineCode") String airlineCode);

    /**
     * Retrieves all upcoming active flights after the specified datetime.
     *
     * @param now the current datetime to compare against
     * @return a list of upcoming flights
     */
    @Query("SELECT f FROM Flight f WHERE f.departureTime > :now AND f.active = true")
    List<Flight> findUpcomingFlights(@Param("now") LocalDateTime now);

    /**
     * The latest departure the timetable currently holds.
     *
     * <p>Asked of the data rather than derived from
     * {@code flights.schedule.horizon-days}, because the two do not agree. The
     * scheduler materialises dates in each airport's own zone, so airports east
     * of UTC push the last date a day past {@code today + horizon}. Recomputing
     * that arithmetic here would mean copying it plus a correction, and going
     * stale the moment the scheduler changes.
     *
     * <p>Null when there are no active flights at all.
     *
     * @return the latest active departure, or null
     */
    @Query("SELECT MAX(f.departureTime) FROM Flight f WHERE f.active = true")
    LocalDateTime findLatestDepartureTime();

    /**
     * Retrieves upcoming active flights arriving at a given airport, from any
     * origin. Backs the "flights to this destination" view, which — unlike
     * search — has no origin to filter on.
     *
     * @param arrivalCode IATA code of the destination airport
     * @param now         the current datetime to compare against
     * @return upcoming arrivals at that airport, earliest departure first
     */
    @Query("SELECT f FROM Flight f WHERE f.arrivalAirport.code = :arrivalCode " +
            "AND f.departureTime > :now AND f.active = true " +
            "ORDER BY f.departureTime ASC")
    List<Flight> findUpcomingArrivals(@Param("arrivalCode") String arrivalCode,
                                      @Param("now") LocalDateTime now);
}
