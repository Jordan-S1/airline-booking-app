package com.airlinebookingsystem.repository;

import com.airlinebookingsystem.entity.Airport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the ranking of {@link AirportRepository#findBySearchQuery}.
 *
 * <p>The ordering is expressed in JPQL, so it cannot be checked with a mocked
 * repository — this runs against a real Postgres with the migrations applied,
 * and asserts on the seeded network the application actually ships.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class AirportRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    private AirportRepository airportRepository;

    private List<String> codesFor(String query) {
        return airportRepository.findBySearchQuery(query).stream().map(Airport::getCode).toList();
    }

    @Test
    @DisplayName("a city match outranks an airport whose name merely contains the term")
    void cityBeatsNameSubstring() {
        // "Josep Tarradellas Barcelona-El Prat" contains "lon", so BCN matches
        // the term as well as London does. It must not come first.
        List<String> codes = codesFor("LON");

        assertThat(codes).contains("LHR", "BCN");
        assertThat(codes.indexOf("LHR")).isLessThan(codes.indexOf("BCN"));
    }

    @Test
    @DisplayName("an exact code match comes first")
    void exactCodeWins() {
        assertThat(codesFor("DUB")).first().isEqualTo("DUB");
    }

    @Test
    @DisplayName("a code match outranks another city that only matches on prefix")
    void codeBeatsCityPrefix() {
        // DUB is the code; Dubai is a city starting with the same letters.
        List<String> codes = codesFor("DUB");

        assertThat(codes).contains("DUB", "DXB");
        assertThat(codes.indexOf("DUB")).isLessThan(codes.indexOf("DXB"));
    }

    @Test
    @DisplayName("a city starting with the term outranks one that only contains it in a name")
    void cityPrefixBeatsNameSubstring() {
        // "Adolfo Suárez Madrid-Barajas" contains "Bar", but Barcelona is the
        // city someone typing it is looking for.
        List<String> codes = codesFor("BAR");

        assertThat(codes).contains("BCN", "MAD");
        assertThat(codes.indexOf("BCN")).isLessThan(codes.indexOf("MAD"));
    }

    @Test
    @DisplayName("matches on any of code, city or name are all still returned")
    void stillMatchesAcrossAllThreeFields() {
        assertThat(codesFor("LHR")).contains("LHR");        // code
        assertThat(codesFor("Madrid")).contains("MAD");     // city
        assertThat(codesFor("Heathrow")).contains("LHR");   // name
    }

    @Test
    @DisplayName("the search is case-insensitive")
    void isCaseInsensitive() {
        assertThat(codesFor("dub")).isEqualTo(codesFor("DUB"));
    }

    @Test
    @DisplayName("a term matching nothing returns nothing rather than everything")
    void unmatchedTermReturnsEmpty() {
        assertThat(codesFor("zzzznowhere")).isEmpty();
    }
}
