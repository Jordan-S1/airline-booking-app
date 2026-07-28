package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.booking.BookingRequest;
import com.airlinebookingsystem.dto.booking.BookingResponse;
import com.airlinebookingsystem.dto.booking.BookingUpdateRequest;
import com.airlinebookingsystem.dto.passenger.PassengerResponse;
import com.airlinebookingsystem.entity.*;
import com.airlinebookingsystem.exception.BookingException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.BookingRepository;
import com.airlinebookingsystem.repository.FlightRepository;
import com.airlinebookingsystem.repository.UserRepository;
import com.airlinebookingsystem.util.SeatClassUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final PassengerService passengerService;

    /**
     * Creates a new booking.
     * If passengers are provided they are created immediately.
     * If no passengers provided, booking is created with numberOfPassengers=0
     * and passengers can be added later via POST /api/v1/passengers/booking/{id}.
     */
    public BookingResponse createBooking(BookingRequest request, Long userId) {
        log.info("Creating booking for user {} on flight {}", userId, request.flightId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Flight flight = flightRepository.findById(request.flightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight", request.flightId()));

        // Determine passenger count — 0 if no passengers provided yet
        int passengerCount = (request.passengers() != null && !request.passengers().isEmpty())
                ? request.passengers().size() : 0;

        // Validate seats only if passengers are being added now
        if (passengerCount > 0) {
            validateSeatAvailability(flight, request.seatClass(), passengerCount);
        }

        BigDecimal totalAmount = calculateTotalAmount(flight, request.seatClass(), passengerCount);

        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .user(user)
                .flight(flight)
                .numberOfPassengers(passengerCount)
                .totalAmount(totalAmount)
                .status(Booking.BookingStatus.PENDING)
                .seatClass(Booking.SeatClass.valueOf(request.seatClass().toUpperCase()))
                .build();

        booking = bookingRepository.save(booking);

        // Create initial passengers if provided
        if (passengerCount > 0) {
            List<PassengerResponse> passengers = passengerService.createPassengers(
                    request.passengers(), booking.getId());
            log.info("Created {} initial passengers for booking {}",
                    passengers.size(), booking.getBookingReference());

            // Update flight seat availability
            updateFlightSeatAvailability(flight, request.seatClass(), passengerCount, false);
            flightRepository.save(flight);
        }

        log.info("Booking created: {}", booking.getBookingReference());
        return mapToBookingResponse(booking);
    }

    /**
     * Updates booking-level fields only — seat class and flight.
     * Passengers are managed separately via the passengers' endpoint.
     */
    public BookingResponse updateBooking(String bookingReference, BookingUpdateRequest request) {
        log.info("Updating booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));
        validateBookingOwnership(booking);

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new BookingException("Can only update PENDING bookings, current status: " + booking.getStatus());
        }

        Flight flight = flightRepository.findById(request.flightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight", request.flightId()));

        // Use provided passenger count or fall back to current count
        int passengerCount = request.numberOfPassengers() != null
                ? request.numberOfPassengers()
                : booking.getNumberOfPassengers();

        boolean seatClassChanged = !booking.getSeatClass().name()
                .equals(request.seatClass().toUpperCase());
        boolean passengerCountChanged = booking.getNumberOfPassengers() != passengerCount;

        if (seatClassChanged || passengerCountChanged) {
            // Restore old seats
            updateFlightSeatAvailability(
                    booking.getFlight(),
                    booking.getSeatClass().name(),
                    booking.getNumberOfPassengers(),
                    true);

            // Validate and reserve new seats
            if (passengerCount > 0) {
                validateSeatAvailability(flight, request.seatClass(), passengerCount);
                updateFlightSeatAvailability(flight, request.seatClass(), passengerCount, false);
            }

            flightRepository.save(flight);
        }

        booking.setFlight(flight);
        booking.setSeatClass(Booking.SeatClass.valueOf(request.seatClass().toUpperCase()));
        booking.setNumberOfPassengers(passengerCount);
        booking.setTotalAmount(calculateTotalAmount(flight, request.seatClass(), passengerCount));

        booking = bookingRepository.save(booking);
        log.info("Booking {} updated — seatClass: {}, passengers: {}, total: {}",
                bookingReference, request.seatClass(), passengerCount, booking.getTotalAmount());
        return mapToBookingResponse(booking);
    }

    public BookingResponse getBookingByReference(String bookingReference) {
        log.info("Retrieving booking: {}", bookingReference);
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));
        validateBookingOwnership(booking);
        return mapToBookingResponse(booking);
    }

    public List<BookingResponse> getBookingsByUserId(Long userId) {
        log.info("Retrieving bookings for user: {}", userId);
        return bookingRepository.findByUserId(userId).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    public BookingResponse confirmBooking(String bookingReference) {
        log.info("Confirming booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));
        validateBookingOwnership(booking);

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            return mapToBookingResponse(booking);
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BookingException("Cannot confirm a cancelled booking: " + bookingReference);
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        log.info("Booking {} confirmed", bookingReference);
        return mapToBookingResponse(bookingRepository.save(booking));
    }

    public BookingResponse cancelBooking(String bookingReference) {
        log.info("Cancelling booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));
        validateBookingOwnership(booking);

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled: " + bookingReference);
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        // Restore seats on the flight
        Flight flight = booking.getFlight();
        updateFlightSeatAvailability(
                flight, booking.getSeatClass().name(), booking.getNumberOfPassengers(), true);
        flightRepository.save(flight);

        log.info("Booking {} cancelled", bookingReference);
        return mapToBookingResponse(booking);
    }

    public List<BookingResponse> getBookingsByStatus(String status) {
        log.info("Retrieving bookings with status: {}", status);
        try {
            Booking.BookingStatus bookingStatus = Booking.BookingStatus.valueOf(status.toUpperCase());
            return bookingRepository.findByStatus(bookingStatus).stream()
                    .map(this::mapToBookingResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid booking status: " + status);
        }
    }

    public List<PassengerResponse> getBookingPassengers(String bookingReference) {
        log.info("Retrieving passengers for booking: {}", bookingReference);
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));
        return passengerService.getPassengersByBookingId(booking.getId());
    }

    // ---- Ownership validation ----

    /**
     * Validates that the current user owns the booking.
     * ADMIN and AIRLINE_STAFF bypass this check.
     */
    private void validateBookingOwnership(Booking booking) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;

        boolean isPrivileged = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_AIRLINE_STAFF"));
        if (isPrivileged) return;

        String currentUserEmail = auth.getName();
        if (!booking.getUser().getEmail().equals(currentUserEmail)) {
            throw new com.airlinebookingsystem.exception.BookingException(
                    "Access denied: you can only manage your own bookings");
        }
    }

    // ---- Private helpers ----

    private void validateSeatAvailability(Flight flight, String seatClass, int requiredSeats) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        SeatClassUtils.validateSeatAvailability(flight, seatClassEnum, requiredSeats);
    }

    private void updateFlightSeatAvailability(
            Flight flight, String seatClass, int seatCount, boolean restore) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        SeatClassUtils.updateFlightSeatAvailability(flight, seatClassEnum, seatCount, restore);
    }

    private String generateBookingReference() {
        String reference;
        do {
            reference = "BK" + System.currentTimeMillis()
                    + String.format("%04d", new Random().nextInt(10000));
        } while (bookingRepository.existsByBookingReference(reference));
        return reference;
    }

    private BigDecimal calculateTotalAmount(Flight flight, String seatClass, int numberOfPassengers) {
        if (numberOfPassengers == 0) return BigDecimal.ZERO;
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        BigDecimal pricePerSeat = SeatClassUtils.getPriceForSeatClass(flight, seatClassEnum);
        return pricePerSeat.multiply(BigDecimal.valueOf(numberOfPassengers));
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getFlight().getId(),
                booking.getFlight().getFlightNumber(),
                booking.getFlight().getDepartureAirport().getCode(),
                booking.getFlight().getArrivalAirport().getCode(),
                booking.getFlight().getDepartureTime(),
                booking.getFlight().getArrivalTime(),
                booking.getNumberOfPassengers(),
                booking.getTotalAmount(),
                booking.getStatus().name(),
                booking.getSeatClass().name(),
                booking.getUser().getEmail(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}