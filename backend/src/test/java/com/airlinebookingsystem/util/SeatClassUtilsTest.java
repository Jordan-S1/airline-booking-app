package com.airlinebookingsystem.util;

import com.airlinebookingsystem.entity.Booking;
import com.airlinebookingsystem.entity.Flight;
import com.airlinebookingsystem.exception.InsufficientSeatsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for SeatClassUtils.
 *
 * Pure functions with no dependencies — no mocking needed.
 * These utilities are used across BookingService, FlightService and
 * PassengerService, so correctness here matters everywhere.
 */
class SeatClassUtilsTest {

    private Flight flight;

    @BeforeEach
    void setUp() {
        flight = Flight.builder()
                .id(1L)
                .flightNumber("EI156")
                .basePrice(new BigDecimal("100.00"))
                .economyPrice(new BigDecimal("89.99"))
                .businessPrice(new BigDecimal("299.99"))
                .firstClassPrice(new BigDecimal("599.99"))
                .totalSeats(190)
                .availableSeats(190)
                .economySeats(150)
                .businessSeats(30)
                .firstClassSeats(10)
                .build();
    }

    @Nested
    @DisplayName("parseSeatClass")
    class ParseSeatClass {

        @ParameterizedTest
        @CsvSource({
                "ECONOMY,  ECONOMY",
                "economy,  ECONOMY",
                "Economy,  ECONOMY",
                "  ECONOMY  , ECONOMY",
                "BUSINESS, BUSINESS",
                "business, BUSINESS",
                "FIRST,    FIRST",
                "first,    FIRST"
        })
        @DisplayName("parses valid seat classes case-insensitively and trims whitespace")
        void parsesValidSeatClasses(String input, Booking.SeatClass expected) {
            assertThat(SeatClassUtils.parseSeatClass(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "PREMIUM", "not-a-class", "123" })
        @DisplayName("falls back to ECONOMY for null, blank or unrecognised input")
        void fallsBackToEconomy(String input) {
            assertThat(SeatClassUtils.parseSeatClass(input))
                    .isEqualTo(Booking.SeatClass.ECONOMY);
        }
    }

    @Nested
    @DisplayName("getPriceForSeatClass")
    class GetPriceForSeatClass {

        @Test
        @DisplayName("returns the class-specific price when set")
        void returnsClassSpecificPrice() {
            assertThat(SeatClassUtils.getPriceForSeatClass(flight, Booking.SeatClass.ECONOMY))
                    .isEqualByComparingTo("89.99");
            assertThat(SeatClassUtils.getPriceForSeatClass(flight, Booking.SeatClass.BUSINESS))
                    .isEqualByComparingTo("299.99");
            assertThat(SeatClassUtils.getPriceForSeatClass(flight, Booking.SeatClass.FIRST))
                    .isEqualByComparingTo("599.99");
        }

        @Test
        @DisplayName("falls back to base price for economy when economyPrice is null")
        void fallsBackToBasePriceForEconomy() {
            flight.setEconomyPrice(null);
            assertThat(SeatClassUtils.getPriceForSeatClass(flight, Booking.SeatClass.ECONOMY))
                    .isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("falls back to 2x base price for business when businessPrice is null")
        void fallsBackToDoubleBaseForBusiness() {
            flight.setBusinessPrice(null);
            assertThat(SeatClassUtils.getPriceForSeatClass(flight, Booking.SeatClass.BUSINESS))
                    .isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("falls back to 3x base price for first class when firstClassPrice is null")
        void fallsBackToTripleBaseForFirst() {
            flight.setFirstClassPrice(null);
            assertThat(SeatClassUtils.getPriceForSeatClass(flight, Booking.SeatClass.FIRST))
                    .isEqualByComparingTo("300.00");
        }
    }

    @Nested
    @DisplayName("hasEnoughSeats")
    class HasEnoughSeats {

        @Test
        @DisplayName("returns true when exactly enough seats remain")
        void trueWhenExactlyEnough() {
            assertThat(SeatClassUtils.hasEnoughSeats(flight, Booking.SeatClass.FIRST, 10)).isTrue();
        }

        @Test
        @DisplayName("returns true when more than enough seats remain")
        void trueWhenMoreThanEnough() {
            assertThat(SeatClassUtils.hasEnoughSeats(flight, Booking.SeatClass.ECONOMY, 5)).isTrue();
        }

        @Test
        @DisplayName("returns false when requesting more seats than remain")
        void falseWhenNotEnough() {
            assertThat(SeatClassUtils.hasEnoughSeats(flight, Booking.SeatClass.FIRST, 11)).isFalse();
        }

        @Test
        @DisplayName("returns false when the seat class has null seats configured")
        void falseWhenSeatsNull() {
            flight.setFirstClassSeats(null);
            assertThat(SeatClassUtils.hasEnoughSeats(flight, Booking.SeatClass.FIRST, 1)).isFalse();
        }

        @Test
        @DisplayName("returns false when the seat class is sold out")
        void falseWhenSoldOut() {
            flight.setBusinessSeats(0);
            assertThat(SeatClassUtils.hasEnoughSeats(flight, Booking.SeatClass.BUSINESS, 1)).isFalse();
        }
    }

    @Nested
    @DisplayName("updateFlightSeatAvailability")
    class UpdateFlightSeatAvailability {

        @Test
        @DisplayName("reserving seats decrements both the class count and total available")
        void reservingDecrementsClassAndTotal() {
            SeatClassUtils.updateFlightSeatAvailability(
                    flight, Booking.SeatClass.BUSINESS, 3, false);

            assertThat(flight.getBusinessSeats()).isEqualTo(27);
            assertThat(flight.getAvailableSeats()).isEqualTo(187);
            // Other classes untouched
            assertThat(flight.getEconomySeats()).isEqualTo(150);
            assertThat(flight.getFirstClassSeats()).isEqualTo(10);
        }

        @Test
        @DisplayName("restoring seats increments both the class count and total available")
        void restoringIncrementsClassAndTotal() {
            SeatClassUtils.updateFlightSeatAvailability(
                    flight, Booking.SeatClass.ECONOMY, 5, true);

            assertThat(flight.getEconomySeats()).isEqualTo(155);
            assertThat(flight.getAvailableSeats()).isEqualTo(195);
        }

        @Test
        @DisplayName("never allows seat counts to go negative")
        void neverGoesNegative() {
            SeatClassUtils.updateFlightSeatAvailability(
                    flight, Booking.SeatClass.FIRST, 999, false);

            assertThat(flight.getFirstClassSeats()).isZero();
            assertThat(flight.getAvailableSeats()).isZero();
        }

        @Test
        @DisplayName("handles null seat counts by treating them as zero")
        void handlesNullSeatCounts() {
            flight.setBusinessSeats(null);
            flight.setAvailableSeats(null);

            SeatClassUtils.updateFlightSeatAvailability(
                    flight, Booking.SeatClass.BUSINESS, 2, true);

            assertThat(flight.getBusinessSeats()).isEqualTo(2);
            assertThat(flight.getAvailableSeats()).isEqualTo(2);
        }

        @Test
        @DisplayName("reserve then restore returns the flight to its original state")
        void reserveThenRestoreIsSymmetric() {
            int originalEconomy = flight.getEconomySeats();
            int originalAvailable = flight.getAvailableSeats();

            SeatClassUtils.updateFlightSeatAvailability(flight, Booking.SeatClass.ECONOMY, 4, false);
            SeatClassUtils.updateFlightSeatAvailability(flight, Booking.SeatClass.ECONOMY, 4, true);

            assertThat(flight.getEconomySeats()).isEqualTo(originalEconomy);
            assertThat(flight.getAvailableSeats()).isEqualTo(originalAvailable);
        }
    }

    @Nested
    @DisplayName("validateSeatAvailability")
    class ValidateSeatAvailability {

        @Test
        @DisplayName("passes silently when enough seats are available")
        void passesWhenEnoughSeats() {
            assertThatCode(() -> SeatClassUtils.validateSeatAvailability(
                    flight, Booking.SeatClass.ECONOMY, 10))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws InsufficientSeatsException when requesting more seats than remain")
        void throwsWhenNotEnoughSeats() {
            assertThatThrownBy(() -> SeatClassUtils.validateSeatAvailability(
                    flight, Booking.SeatClass.FIRST, 11))
                    .isInstanceOf(InsufficientSeatsException.class)
                    .hasMessageContaining("first")
                    .hasMessageContaining("requested 11")
                    .hasMessageContaining("available 10");
        }

        @Test
        @DisplayName("reports zero available when the seat count is null")
        void reportsZeroWhenSeatsNull() {
            flight.setBusinessSeats(null);

            assertThatThrownBy(() -> SeatClassUtils.validateSeatAvailability(
                    flight, Booking.SeatClass.BUSINESS, 1))
                    .isInstanceOf(InsufficientSeatsException.class)
                    .hasMessageContaining("available 0");
        }
    }
}
