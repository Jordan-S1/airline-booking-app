package com.airlinebookingsystem.repository;

import com.airlinebookingsystem.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Airport entities in the database.
 * Provides CRUD operations and custom query methods for airports.
 */
@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {

    /**
     * Finds an airport by its unique code.
     *
     * @param code the airport code to search for
     * @return an Optional containing the airport if found, or empty if not found
     */
    Optional<Airport> findByCode(String code);

    /**
     * Searches for airports matching a given query string in city, name, or code fields.
     * The search is case-insensitive and uses partial matching.
     *
     * Results are ranked, because matching on three fields at once makes
     * where the match landed matter more than that it matched. Ordered:
     * an exact code, then a code prefix, then a city starting with the term,
     * then a city containing it, and last anything that only matched the
     * airport's full name.
     *
     * @param query the search string to match against airport fields
     * @return matching airports, most relevant first
     */
    @Query("SELECT a FROM Airport a WHERE " +
            "LOWER(a.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.code) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY CASE " +
            "WHEN LOWER(a.code) = LOWER(:query) THEN 0 " +
            "WHEN LOWER(a.code) LIKE LOWER(CONCAT(:query, '%')) THEN 1 " +
            "WHEN LOWER(a.city) LIKE LOWER(CONCAT(:query, '%')) THEN 2 " +
            "WHEN LOWER(a.city) LIKE LOWER(CONCAT('%', :query, '%')) THEN 3 " +
            "ELSE 4 END, a.city ASC, a.code ASC")
    List<Airport> findBySearchQuery(@Param("query") String query);

    /**
     * Retrieves all airports in a specific country, ordered by city name.
     *
     * @param country the country to search for airports in
     * @return a list of airports in the specified country, ordered by city name
     */
    List<Airport> findByCountryOrderByCity(String country);
}