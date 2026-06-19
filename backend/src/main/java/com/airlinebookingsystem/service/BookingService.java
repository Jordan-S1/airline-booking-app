package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.BookingRequest;
import com.airlinebookingsystem.dto.BookingResponse;
import com.airlinebookingsystem.dto.PassengerResponse;
import com.airlinebookingsystem.entity.*;
import com.airlinebookingsystem.exception.BookingException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.BookingRepository;
import com.airlinebookingsystem.repository.FlightRepository;
import com.airlinebookingsystem.repository.UserRepository;
import com.airlinebookingsystem.util.SeatClassUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * Creates a new booking based on the provided booking request.
     *
     * @param request the booking request containing flight and passenger details
     * @param userId  the ID of the user making the booking
     * @return BookingResponse containing the created booking details
     * @throws ResourceNotFoundException if flight or user is not found, or if
     *                                   insufficient seats are available
     */
    public BookingResponse createBooking(BookingRequest request, Long userId) {
        log.info("Creating booking for user {} and flight {}", userId, request.flightId());

        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Validate flight
        Flight flight = flightRepository.findById(request.flightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight", request.flightId()));

        // Check seat availability based on seat class
        validateSeatAvailability(flight, request.seatClass(), request.passengers().size());

        // Calculate total amount based on seat class
        BigDecimal totalAmount = calculateTotalAmount(flight, request.seatClass(), request.passengers().size());

        // Create a booking entity
        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .user(user)
                .flight(flight)
                .numberOfPassengers(request.passengers().size())
                .totalAmount(totalAmount)
                .status(Booking.BookingStatus.PENDING)
                .seatClass(Booking.SeatClass.valueOf(request.seatClass()))
                .build();

        // Save booking first to get ID
        booking = bookingRepository.save(booking);

        // Create passengers using PassengerService
        List<PassengerResponse> createdPassengers = passengerService.createPassengers(request.passengers(),
                booking.getId());
        log.info("Created {} passengers for booking {}", createdPassengers.size(), booking.getBookingReference());

        // Update flight availability based on seat class
        updateFlightSeatAvailability(flight, request.seatClass(), request.passengers().size(), false);
        flightRepository.save(flight);

        log.info("Booking created successfully: {}", booking.getBookingReference());
        return mapToBookingResponse(booking);
    }

    /**
     * Retrieves a booking by its reference number.
     *
     * @param bookingReference the booking reference to search for
     * @return BookingResponse containing the booking details
     * @throws ResourceNotFoundException if the booking is not found
     */
    public BookingResponse getBookingByReference(String bookingReference) {
        log.info("Retrieving booking: {}", bookingReference);
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));
        return mapToBookingResponse(booking);
    }

    /**
     * Retrieves all bookings for a specific user.
     *
     * @param userId the ID of the user
     * @return List of BookingResponse objects
     */
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        log.info("Retrieving bookings for user: {}", userId);
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Confirms a booking by updating its status to CONFIRMED.
     *
     * @param bookingReference the booking reference to confirm
     * @return BookingResponse with updated status
     * @throws ResourceNotFoundException if the booking is not found
     */
    public BookingResponse confirmBooking(String bookingReference) {
        log.info("Confirming booking: {}", bookingReference);
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            log.warn("Booking {} is already confirmed", bookingReference);
            return mapToBookingResponse(booking);
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BookingException("Cannot confirm a cancelled booking: " + bookingReference);
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        log.info("Booking {} confirmed successfully", bookingReference);
        return mapToBookingResponse(booking);
    }

    /**
     * Cancels a booking and restores flight seat availability.
     *
     * @param bookingReference the booking reference to cancel
     * @return BookingResponse with updated status
     * @throws ResourceNotFoundException if the booking is not found or already
     *                                   cancelled
     */
    public BookingResponse cancelBooking(String bookingReference) {
        log.info("Cancelling booking: {}", bookingReference);
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled: " + bookingReference);
        }

        // Update booking status
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        // Restore flight seat availability
        Flight flight = booking.getFlight();
        updateFlightSeatAvailability(flight, booking.getSeatClass().name(), booking.getNumberOfPassengers(), true);
        flightRepository.save(flight);

        log.info("Booking {} cancelled successfully", bookingReference);
        return mapToBookingResponse(booking);
    }

    /**
     * Retrieves all bookings with a specific status.
     *
     * @param status the booking status to filter by
     * @return List of BookingResponse objects
     */
    public List<BookingResponse> getBookingsByStatus(String status) {
        log.info("Retrieving bookings with status: {}", status);
        try {
            Booking.BookingStatus bookingStatus = Booking.BookingStatus.valueOf(status.toUpperCase());
            List<Booking> bookings = bookingRepository.findByStatus(bookingStatus);
            return bookings.stream()
                    .map(this::mapToBookingResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid booking status: " + status);
        }
    }

    /**
     * Retrieves passengers for a specific booking.
     *
     * @param bookingReference the booking reference
     * @return List of PassengerResponse objects
     */
    public List<PassengerResponse> getBookingPassengers(String bookingReference) {
        log.info("Retrieving passengers for booking: {}", bookingReference);
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));
        return passengerService.getPassengersByBookingId(booking.getId());
    }

    /**
     * Updates an existing booking (limited updates allowed).
     *
     * @param bookingReference the booking reference to update
     * @param request          new booking details
     * @return updated BookingResponse
     */
    public BookingResponse updateBooking(String bookingReference, BookingRequest request) {
        log.info("Updating booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));

        // Only allow updates for pending bookings
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new BookingException("Can only update PENDING bookings, current status: " + booking.getStatus());
        }

        // Update seat class if changed
        if (!booking.getSeatClass().name().equals(request.seatClass().toUpperCase())) {
            // Restore old seat availability and check new seat availability
            Flight flight = booking.getFlight();
            updateFlightSeatAvailability(flight, booking.getSeatClass().name(), booking.getNumberOfPassengers(), true);
            validateSeatAvailability(flight, request.seatClass(), request.passengers().size());
            updateFlightSeatAvailability(flight, request.seatClass(), request.passengers().size(), false);

            booking.setSeatClass(Booking.SeatClass.valueOf(request.seatClass().toUpperCase()));
            booking.setTotalAmount(calculateTotalAmount(flight, request.seatClass(), request.passengers().size()));

            flightRepository.save(flight);
        }

        booking = bookingRepository.save(booking);
        log.info("Booking {} updated successfully", bookingReference);
        return mapToBookingResponse(booking);
    }

    /**
     * Validates seat availability for a specific seat class.
     */
    private void validateSeatAvailability(Flight flight, String seatClass, int requiredSeats) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        SeatClassUtils.validateSeatAvailability(flight, seatClassEnum, requiredSeats);
    }

    /**
     * Updates flight seat availability based on seat class.
     */
    private void updateFlightSeatAvailability(Flight flight, String seatClass, int seatCount, boolean restore) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        SeatClassUtils.updateFlightSeatAvailability(flight, seatClassEnum, seatCount, restore);
    }

    /**
     * Generates a unique booking reference.
     *
     * @return a unique booking reference string
     */
    private String generateBookingReference() {
        String reference;
        do {
            reference = "BK" + System.currentTimeMillis() +
                    String.format("%04d", new Random().nextInt(10000));
        } while (bookingRepository.existsByBookingReference(reference));
        return reference;
    }

    /**
     * Calculates the total amount for a booking based on seat class and number of
     * passengers.
     *
     * @param flight             the flight being booked
     * @param seatClass          the seat class requested
     * @param numberOfPassengers the number of passengers
     * @return the total amount for the booking
     */
    private BigDecimal calculateTotalAmount(Flight flight, String seatClass, int numberOfPassengers) {
        Booking.SeatClass seatClassEnum = SeatClassUtils.parseSeatClass(seatClass);
        BigDecimal pricePerSeat = SeatClassUtils.getPriceForSeatClass(flight, seatClassEnum);
        return pricePerSeat.multiply(BigDecimal.valueOf(numberOfPassengers));
    }

    /**
     * Maps a Booking entity to a BookingResponse DTO.
     *
     * @param booking the booking entity to map
     * @return BookingResponse DTO
     */
    private BookingResponse mapToBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
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
                booking.getUpdatedAt());
    }
}