package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.PassengerRequest;
import com.airlinebookingsystem.dto.PassengerResponse;
import com.airlinebookingsystem.entity.Booking;
import com.airlinebookingsystem.entity.Passenger;
import com.airlinebookingsystem.repository.BookingRepository;
import com.airlinebookingsystem.repository.PassengerRepository;
import com.airlinebookingsystem.exception.BookingException;
import com.airlinebookingsystem.exception.DuplicateResourceException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing passenger-related operations in the airline
 * booking system.
 * Provides methods for creating, retrieving, updating, and deleting passenger
 * records.
 * All operations are transactional and interact with the PassengerRepository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final BookingRepository bookingRepository;

    /**
     * Creates a new passenger record associated with a booking.
     *
     * @param passengerRequest the passenger details to create
     * @param bookingId        the ID of the booking to associate with
     * @return PassengerResponse containing the created passenger details
     * @throws ResourceNotFoundException if a booking is not found
     */
    public PassengerResponse createPassenger(PassengerRequest passengerRequest, Long bookingId) {
        log.info("Creating passenger for booking: {}", bookingId);

        validatePassengerRequest(passengerRequest);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // Check for duplicate passport in the same booking
        checkDuplicatePassportInBooking(passengerRequest.passportNumber(), bookingId);

        // Validate booking status allows passenger addition
        validateBookingForPassengerOperation(booking);

        Passenger passenger = buildPassengerFromRequest(passengerRequest, booking);
        passenger = passengerRepository.save(passenger);

        log.info("Passenger created successfully with ID: {}", passenger.getId());
        return mapToPassengerResponse(passenger);
    }

    /**
     * Creates multiple passengers for a booking.
     *
     * @param passengerRequests list of passenger details to create
     * @param bookingId         the ID of the booking to associate with
     * @return List of PassengerResponse containing the created passengers
     */
    public List<PassengerResponse> createPassengers(List<PassengerRequest> passengerRequests, Long bookingId) {
        log.info("Creating {} passengers for booking: {}", passengerRequests.size(), bookingId);

        // Check for duplicate passports within the request
        List<String> passportNumbers = passengerRequests.stream()
                .map(PassengerRequest::passportNumber)
                .toList();

        long uniquePassports = passportNumbers.stream().distinct().count();
        if (uniquePassports != passportNumbers.size()) {
            throw new IllegalArgumentException("Duplicate passport numbers found in passenger list");
        }

        return passengerRequests.stream()
                .map(request -> createPassenger(request, bookingId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a passenger by their ID.
     *
     * @param id the ID of the passenger to retrieve
     * @return PassengerResponse containing the passenger details
     * @throws ResourceNotFoundException if the passenger is not found
     */
    public PassengerResponse getPassengerById(Long id) {
        log.info("Retrieving passenger with ID: {}", id);
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", id));
        return mapToPassengerResponse(passenger);
    }

    /**
     * Retrieves all passengers associated with a specific booking.
     *
     * @param bookingId the ID of the booking
     * @return List of PassengerResponse objects
     */
    public List<PassengerResponse> getPassengersByBookingId(Long bookingId) {
        log.info("Retrieving passengers for booking: {}", bookingId);
        List<Passenger> passengers = passengerRepository.findByBookingId(bookingId);
        return passengers.stream()
                .map(this::mapToPassengerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all passengers booked on a specific flight.
     *
     * @param flightId the ID of the flight
     * @return List of PassengerResponse objects
     */
    public List<PassengerResponse> getPassengersByFlightId(Long flightId) {
        log.info("Retrieving passengers for flight: {}", flightId);
        List<Passenger> passengers = passengerRepository.findByFlightId(flightId);
        return passengers.stream()
                .map(this::mapToPassengerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Finds passengers by their passport number.
     *
     * @param passportNumber the passport number to search for
     * @return List of PassengerResponse objects
     */
    public List<PassengerResponse> getPassengersByPassportNumber(String passportNumber) {
        log.info("Searching passengers by passport number: {}", passportNumber);
        List<Passenger> passengers = passengerRepository.findByPassportNumber(passportNumber);
        return passengers.stream()
                .map(this::mapToPassengerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all passengers in the system.
     *
     * @return List of all PassengerResponse objects
     */
    public List<PassengerResponse> getAllPassengers() {
        log.info("Retrieving all passengers");
        List<Passenger> passengers = passengerRepository.findAll();
        return passengers.stream()
                .map(this::mapToPassengerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets passenger count by booking ID.
     *
     * @param bookingId the booking ID
     * @return count of passengers for the booking
     */
    public long getPassengerCountByBooking(Long bookingId) {
        return passengerRepository.findByBookingId(bookingId).size();
    }

    /**
     * Updates passenger information.
     *
     * @param id               the ID of the passenger to update
     * @param passengerRequest the updated passenger details
     * @return PassengerResponse containing the updated passenger details
     * @throws ResourceNotFoundException if the passenger is not found
     */
    public PassengerResponse updatePassenger(Long id, PassengerRequest passengerRequest) {
        log.info("Updating passenger with ID: {}", id);

        validatePassengerRequest(passengerRequest);

        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", id));

        // Validate booking allows updates
        validateBookingForPassengerOperation(passenger.getBooking());

        // Check for passport conflicts if the passport is being changed
        if (!passenger.getPassportNumber().equals(passengerRequest.passportNumber())) {
            checkDuplicatePassportInBooking(passengerRequest.passportNumber(), passenger.getBooking().getId());
        }

        updatePassengerFields(passenger, passengerRequest);
        passenger = passengerRepository.save(passenger);

        log.info("Passenger updated successfully with ID: {}", passenger.getId());
        return mapToPassengerResponse(passenger);
    }

    /**
     * Assigns a seat to a passenger.
     *
     * @param passengerId the ID of the passenger
     * @param seatNumber  the seat number to assign
     * @return PassengerResponse containing the updated passenger details
     * @throws ResourceNotFoundException if the passenger is not found
     */
    public PassengerResponse assignSeat(Long passengerId, String seatNumber) {
        log.info("Assigning seat {} to passenger: {}", seatNumber, passengerId);
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", passengerId));

        // Check if a seat is already assigned to another passenger on the same flight
        validateSeatAvailability(passenger.getBooking().getFlight().getId(), seatNumber, passengerId);

        passenger.setSeatNumber(seatNumber.trim().toUpperCase());
        passenger = passengerRepository.save(passenger);

        log.info("Seat assigned successfully to passenger: {}", passengerId);
        return mapToPassengerResponse(passenger);
    }

    /**
     * Removes a passenger from the system.
     *
     * @param id the ID of the passenger to delete
     * @throws ResourceNotFoundException if a passenger is not found
     */
    public void deletePassenger(Long id) {
        log.info("Deleting passenger with ID: {}", id);
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", id));

        validateBookingForPassengerOperation(passenger.getBooking());

        passengerRepository.deleteById(id);
        log.info("Passenger deleted successfully with ID: {}", id);
    }

    // Private helper methods

    /**
     * Validates passenger request data.
     */
    private void validatePassengerRequest(PassengerRequest request) {
        if (!StringUtils.hasText(request.firstName())) {
            throw new IllegalArgumentException("First name is required");
        }
        if (!StringUtils.hasText(request.lastName())) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (request.dateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }
        if (request.dateOfBirth().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        if (!StringUtils.hasText(request.passportNumber())) {
            throw new IllegalArgumentException("Passport number is required");
        }
        if (!StringUtils.hasText(request.nationality())) {
            throw new IllegalArgumentException("Nationality is required");
        }

        // Validate age restrictions
        int age = Period.between(request.dateOfBirth(), LocalDate.now()).getYears();
        if (age > 120) {
            throw new IllegalArgumentException("Invalid date of birth - age cannot exceed 120 years");
        }
    }

    /**
     * Checks for duplicate passport numbers within the same booking.
     */
    private void checkDuplicatePassportInBooking(String passportNumber, Long bookingId) {
        List<Passenger> existingPassengers = passengerRepository.findByBookingId(bookingId);
        boolean duplicateExists = existingPassengers.stream()
                .anyMatch(p -> p.getPassportNumber().equalsIgnoreCase(passportNumber.trim()));

        if (duplicateExists) {
            throw new DuplicateResourceException("Passport number", passportNumber);
        }
    }

    /**
     * Validates that the booking allows passenger operations.
     */
    private void validateBookingForPassengerOperation(Booking booking) {
        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new BookingException("Cannot modify passengers in confirmed booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BookingException("Cannot modify passengers in cancelled booking");
        }
    }

    /**
     * Validates seat availability on the flight.
     */
    private void validateSeatAvailability(Long flightId, String seatNumber, Long currentPassengerId) {
        List<Passenger> flightPassengers = passengerRepository.findByFlightId(flightId);
        Optional<Passenger> conflictingPassenger = flightPassengers.stream()
                .filter(p -> seatNumber.equalsIgnoreCase(p.getSeatNumber()) &&
                        !p.getId().equals(currentPassengerId))
                .findFirst();

        if (conflictingPassenger.isPresent()) {
            throw new DuplicateResourceException("Seat", seatNumber);
        }
    }

    /**
     * Builds a Passenger entity from a PassengerRequest.
     */
    private Passenger buildPassengerFromRequest(PassengerRequest request, Booking booking) {
        return Passenger.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(Passenger.Gender.valueOf(request.gender().toUpperCase()))
                .passportNumber(request.passportNumber().trim().toUpperCase())
                .nationality(request.nationality().trim())
                .passengerType(request.passengerType() != null
                        ? Passenger.PassengerType.valueOf(request.passengerType().toUpperCase())
                        : determinePassengerType(request.dateOfBirth()))
                .booking(booking)
                .build();
    }

    /**
     * Updates passenger fields from request.
     */
    private void updatePassengerFields(Passenger passenger, PassengerRequest request) {
        passenger.setFirstName(request.firstName().trim());
        passenger.setLastName(request.lastName().trim());
        passenger.setDateOfBirth(request.dateOfBirth());
        passenger.setGender(Passenger.Gender.valueOf(request.gender().toUpperCase()));
        passenger.setPassportNumber(request.passportNumber().trim().toUpperCase());
        passenger.setNationality(request.nationality().trim());

        if (request.passengerType() != null) {
            passenger.setPassengerType(Passenger.PassengerType.valueOf(request.passengerType().toUpperCase()));
        } else {
            passenger.setPassengerType(determinePassengerType(request.dateOfBirth()));
        }
    }

    /**
     * Determines a passenger type based on age.
     */
    private Passenger.PassengerType determinePassengerType(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < 2)
            return Passenger.PassengerType.INFANT;
        if (age < 12)
            return Passenger.PassengerType.CHILD;
        return Passenger.PassengerType.ADULT;
    }

    /**
     * Maps a Passenger entity to a PassengerResponse DTO.
     *
     * @param passenger the passenger entity to map
     * @return PassengerResponse DTO
     */
    private PassengerResponse mapToPassengerResponse(Passenger passenger) {
        return new PassengerResponse(
                passenger.getId(),
                passenger.getFirstName(),
                passenger.getLastName(),
                passenger.getDateOfBirth(),
                passenger.getGender().name(),
                passenger.getPassportNumber(),
                passenger.getNationality(),
                passenger.getSeatNumber(),
                passenger.getPassengerType().name(),
                passenger.getBooking().getId(),
                passenger.getBooking().getBookingReference(),
                passenger.getBooking().getFlight().getFlightNumber(),
                passenger.getCreatedAt(),
                passenger.getUpdatedAt());
    }
}