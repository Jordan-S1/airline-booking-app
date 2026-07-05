package com.airlinebookingsystem.util;

import com.airlinebookingsystem.entity.Booking;
import com.airlinebookingsystem.entity.Flight;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

@UtilityClass
public class SeatClassUtils {
    /**
     * Converts string seat class to enum, with validation
     */
    public static Booking.SeatClass parseSeatClass(String seatClassStr) {
        if (seatClassStr == null || seatClassStr.trim().isEmpty()) {
            return Booking.SeatClass.ECONOMY;
        }

        try {
            return Booking.SeatClass.valueOf(seatClassStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return Booking.SeatClass.ECONOMY; // default fallback
        }
    }

    /**
     * Gets the price for a specific seat class, with fallback logic
     */
    public static BigDecimal getPriceForSeatClass(Flight flight, Booking.SeatClass seatClass) {
        return switch (seatClass) {
            case ECONOMY -> flight.getEconomyPrice() != null ?
                    flight.getEconomyPrice() : flight.getBasePrice();
            case BUSINESS -> flight.getBusinessPrice() != null ?
                    flight.getBusinessPrice() : flight.getBasePrice().multiply(BigDecimal.valueOf(2));
            case FIRST -> flight.getFirstClassPrice() != null ?
                    flight.getFirstClassPrice() : flight.getBasePrice().multiply(BigDecimal.valueOf(3));
        };
    }

    /**
     * Gets available seats for a specific seat class
     */
    public static Integer getAvailableSeatsForClass(Flight flight, Booking.SeatClass seatClass) {
        return switch (seatClass) {
            case ECONOMY -> flight.getEconomySeats();
            case BUSINESS -> flight.getBusinessSeats();
            case FIRST -> flight.getFirstClassSeats();
        };
    }

    /**
     * Checks if flight has enough seats for the requested class
     */
    public static boolean hasEnoughSeats(Flight flight, Booking.SeatClass seatClass, int requiredSeats) {
        Integer availableSeats = getAvailableSeatsForClass(flight, seatClass);
        return availableSeats != null && availableSeats >= requiredSeats;
    }

    /**
     * Updates flight seat availability for a specific class
     */
    public static void updateFlightSeatAvailability(Flight flight, Booking.SeatClass seatClass, int seatCount, boolean restore) {
        int change = restore ? seatCount : -seatCount;

        switch (seatClass) {
            case ECONOMY -> {
                int newCount = (flight.getEconomySeats() != null ? flight.getEconomySeats() : 0) + change;
                flight.setEconomySeats(Math.max(0, newCount));
            }
            case BUSINESS -> {
                int newCount = (flight.getBusinessSeats() != null ? flight.getBusinessSeats() : 0) + change;
                flight.setBusinessSeats(Math.max(0, newCount));
            }
            case FIRST -> {
                int newCount = (flight.getFirstClassSeats() != null ? flight.getFirstClassSeats() : 0) + change;
                flight.setFirstClassSeats(Math.max(0, newCount));
            }
        }

        // Always keep availableSeats in sync — it reflects total remaining seats across all classes
        int newAvailable = (flight.getAvailableSeats() != null ? flight.getAvailableSeats() : 0) + change;
        flight.setAvailableSeats(Math.max(0, newAvailable));
    }

    /**
     * Validates seat availability and throws exception if insufficient
     */
    public static void validateSeatAvailability(Flight flight, Booking.SeatClass seatClass, int requiredSeats) {
        if (!hasEnoughSeats(flight, seatClass, requiredSeats)) {
            Integer availableSeats = getAvailableSeatsForClass(flight, seatClass);
            throw new RuntimeException(String.format(
                    "Insufficient %s class seats available. Required: %d, Available: %d",
                    seatClass.name().toLowerCase(), requiredSeats, availableSeats != null ? availableSeats : 0));
        }
    }
}
