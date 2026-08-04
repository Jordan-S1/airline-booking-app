package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.booking.BookingRequest;
import com.airlinebookingsystem.dto.booking.BookingResponse;
import com.airlinebookingsystem.dto.booking.BookingUpdateRequest;
import com.airlinebookingsystem.dto.passenger.PassengerRequest;
import com.airlinebookingsystem.dto.passenger.PassengerResponse;
import com.airlinebookingsystem.entity.*;
import com.airlinebookingsystem.exception.BookingException;
import com.airlinebookingsystem.exception.InsufficientSeatsException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.BookingRepository;
import com.airlinebookingsystem.repository.FlightRepository;
import com.airlinebookingsystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService.
 *
 * BookingService holds the most complex business rules in the system:
 * seat inventory, ownership enforcement, status transitions, pricing,
 * and refund-before-cancel ordering. All collaborators are mocked so
 * each rule can be exercised in isolation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private FlightRepository flightRepository;
    @Mock private UserRepository userRepository;
    @Mock private PassengerService passengerService;
    @Mock private PaymentSettlementService paymentSettlementService;

    @InjectMocks private BookingService bookingService;

    private User customer;
    private Flight flight;
    private Booking booking;

    private static final String REF = "BK17832167460316843";
    private static final String CUSTOMER_EMAIL = "jordan@example.com";

    @BeforeEach
    void setUp() {
        Airport dublin = Airport.builder()
                .id(1L).code("DUB").name("Dublin Airport")
                .city("Dublin").country("Ireland").timezone("Europe/Dublin")
                .build();

        Airport heathrow = Airport.builder()
                .id(2L).code("LHR").name("Heathrow Airport")
                .city("London").country("United Kingdom").timezone("Europe/London")
                .build();

        Airline aerLingus = Airline.builder()
                .id(1L).code("EI").name("Aer Lingus").country("Ireland").active(true)
                .build();

        flight = Flight.builder()
                .id(1L)
                .flightNumber("EI156")
                .airline(aerLingus)
                .departureAirport(dublin)
                .arrivalAirport(heathrow)
                .departureTime(LocalDateTime.of(2026, 8, 1, 6, 30))
                .arrivalTime(LocalDateTime.of(2026, 8, 1, 7, 55))
                .duration(85)
                .basePrice(new BigDecimal("89.99"))
                .economyPrice(new BigDecimal("89.99"))
                .businessPrice(new BigDecimal("299.99"))
                .firstClassPrice(new BigDecimal("599.99"))
                .totalSeats(190).availableSeats(190)
                .economySeats(150).businessSeats(30).firstClassSeats(10)
                .status(Flight.FlightStatus.SCHEDULED)
                .active(true)
                .build();

        customer = User.builder()
                .id(1L)
                .email(CUSTOMER_EMAIL)
                .firstName("Jordan").lastName("Test")
                .password("hashed")
                .role(User.Role.CUSTOMER)
                .build();

        booking = Booking.builder()
                .id(1L)
                .bookingReference(REF)
                .user(customer)
                .flight(flight)
                .numberOfPassengers(1)
                .totalAmount(new BigDecimal("89.99"))
                .status(Booking.BookingStatus.PENDING)
                .seatClass(Booking.SeatClass.ECONOMY)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---- Helpers ----

    private void authenticateAs(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PassengerRequest samplePassenger(String passport) {
        return new PassengerRequest(
                "Jordan", "Test", LocalDate.of(2001, 1, 1),
                "MALE", passport, "Irish");
    }

    // ---- createBooking ----

    @Nested
    @DisplayName("createBooking")
    class CreateBooking {

        @Test
        @DisplayName("creates a PENDING booking with zero passengers when none are supplied")
        void createsEmptyBooking() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
            when(bookingRepository.existsByBookingReference(anyString())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            BookingRequest request = new BookingRequest(1L, "ECONOMY", null);
            BookingResponse response = bookingService.createBooking(request, 1L);

            assertThat(response.numberOfPassengers()).isZero();
            assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.seatClass()).isEqualTo("ECONOMY");

            // No seats should be reserved yet
            assertThat(flight.getEconomySeats()).isEqualTo(150);
            verify(passengerService, never()).createPassengers(anyList(), anyLong());
        }

        @Test
        @DisplayName("reserves seats and calculates the total when passengers are supplied")
        void createsBookingWithPassengers() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
            when(bookingRepository.existsByBookingReference(anyString())).thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
                Booking b = inv.getArgument(0);
                b.setId(1L);
                return b;
            });
            when(passengerService.createPassengers(anyList(), anyLong()))
                    .thenReturn(List.of(mock(PassengerResponse.class), mock(PassengerResponse.class)));

            BookingRequest request = new BookingRequest(1L, "BUSINESS",
                    List.of(samplePassenger("PA111"), samplePassenger("PA222")));

            BookingResponse response = bookingService.createBooking(request, 1L);

            assertThat(response.numberOfPassengers()).isEqualTo(2);
            // 299.99 × 2
            assertThat(response.totalAmount()).isEqualByComparingTo("599.98");

            // Two business seats reserved
            assertThat(flight.getBusinessSeats()).isEqualTo(28);
            assertThat(flight.getAvailableSeats()).isEqualTo(188);
            verify(flightRepository).save(flight);
        }

        @Test
        @DisplayName("generates a unique booking reference, retrying on collision")
        void retriesOnReferenceCollision() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
            // First generated reference collides, second is free
            when(bookingRepository.existsByBookingReference(anyString()))
                    .thenReturn(true, false);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            bookingService.createBooking(new BookingRequest(1L, "ECONOMY", null), 1L);

            verify(bookingRepository, times(2)).existsByBookingReference(anyString());
        }

        @Test
        @DisplayName("throws when the user does not exist")
        void throwsWhenUserMissing() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    bookingService.createBooking(new BookingRequest(1L, "ECONOMY", null), 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("throws when the flight does not exist")
        void throwsWhenFlightMissing() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(flightRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    bookingService.createBooking(new BookingRequest(99L, "ECONOMY", null), 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Flight");
        }

        @Test
        @DisplayName("rejects the booking when the cabin does not have enough seats")
        void rejectsWhenInsufficientSeats() {
            flight.setFirstClassSeats(1);
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

            BookingRequest request = new BookingRequest(1L, "FIRST",
                    List.of(samplePassenger("PA111"), samplePassenger("PA222")));

            assertThatThrownBy(() -> bookingService.createBooking(request, 1L))
                    .isInstanceOf(InsufficientSeatsException.class)
                    .hasMessageContaining("requested 2")
                    .hasMessageContaining("available 1");

            verify(bookingRepository, never()).save(any());
        }
    }

    // ---- confirmBooking ----

    @Nested
    @DisplayName("confirmBooking")
    class ConfirmBooking {

        @Test
        @DisplayName("moves a PENDING booking to CONFIRMED")
        void confirmsPendingBooking() {
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            BookingResponse response = bookingService.confirmBooking(REF);

            assertThat(response.status()).isEqualTo("CONFIRMED");
            assertThat(booking.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("is idempotent — confirming an already CONFIRMED booking is a no-op")
        void confirmingTwiceIsIdempotent() {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));

            BookingResponse response = bookingService.confirmBooking(REF);

            assertThat(response.status()).isEqualTo("CONFIRMED");
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses to confirm a CANCELLED booking")
        void refusesToConfirmCancelled() {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.confirmBooking(REF))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("Cannot confirm a cancelled booking");
        }

        @Test
        @DisplayName("throws when the booking reference is unknown")
        void throwsWhenBookingMissing() {
            when(bookingRepository.findByBookingReference("NOPE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.confirmBooking("NOPE"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---- cancelBooking ----

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("cancels the booking and returns the seats to the flight")
        void cancelsAndRestoresSeats() {
            booking.setNumberOfPassengers(2);
            booking.setSeatClass(Booking.SeatClass.BUSINESS);
            flight.setBusinessSeats(28);
            flight.setAvailableSeats(188);

            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentSettlementService.refundIfCharged(booking)).thenReturn(Optional.empty());

            BookingResponse response = bookingService.cancelBooking(REF);

            assertThat(response.status()).isEqualTo("CANCELLED");
            assertThat(flight.getBusinessSeats()).isEqualTo(30);
            assertThat(flight.getAvailableSeats()).isEqualTo(190);
            verify(flightRepository).save(flight);
        }

        @Test
        @DisplayName("refunds any charge before releasing the seat")
        void refundsBeforeReleasingSeat() {
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentSettlementService.refundIfCharged(booking))
                    .thenReturn(Optional.of(mock(Payment.class)));

            bookingService.cancelBooking(REF);

            InOrder order = inOrder(paymentSettlementService, flightRepository);
            order.verify(paymentSettlementService).refundIfCharged(booking);
            order.verify(flightRepository).save(flight);
        }

        @Test
        @DisplayName("aborts the cancellation when the refund fails")
        void abortsWhenRefundFails() {
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));
            when(paymentSettlementService.refundIfCharged(booking))
                    .thenThrow(new BookingException("Refund processing failed"));

            assertThatThrownBy(() -> bookingService.cancelBooking(REF))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("Refund processing failed");

            // Booking must not be marked cancelled and seats must not be released
            assertThat(booking.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
            verify(flightRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses to cancel an already CANCELLED booking")
        void refusesDoubleCancel() {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(REF))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("already cancelled");

            verify(paymentSettlementService, never()).refundIfCharged(any());
        }
    }

    // ---- updateBooking ----

    @Nested
    @DisplayName("updateBooking")
    class UpdateBooking {

        @Test
        @DisplayName("switching cabin releases the old seats and reserves the new ones")
        void switchingCabinMovesSeats() {
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            // Booking currently holds 1 economy seat
            flight.setEconomySeats(149);
            flight.setAvailableSeats(189);

            BookingUpdateRequest request = new BookingUpdateRequest(1L, "BUSINESS", null);
            BookingResponse response = bookingService.updateBooking(REF, request);

            assertThat(response.seatClass()).isEqualTo("BUSINESS");
            assertThat(response.totalAmount()).isEqualByComparingTo("299.99");
            assertThat(flight.getEconomySeats()).isEqualTo(150); // released
            assertThat(flight.getBusinessSeats()).isEqualTo(29); // reserved
        }

        @Test
        @DisplayName("changing the passenger count recalculates the total")
        void changingPassengerCountRecalculatesTotal() {
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            BookingUpdateRequest request = new BookingUpdateRequest(1L, "ECONOMY", 3);
            BookingResponse response = bookingService.updateBooking(REF, request);

            assertThat(response.numberOfPassengers()).isEqualTo(3);
            assertThat(response.totalAmount()).isEqualByComparingTo("269.97"); // 89.99 × 3
        }

        @Test
        @DisplayName("keeps the current passenger count when none is supplied")
        void keepsPassengerCountWhenOmitted() {
            booking.setNumberOfPassengers(2);
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            BookingUpdateRequest request = new BookingUpdateRequest(1L, "ECONOMY", null);
            BookingResponse response = bookingService.updateBooking(REF, request);

            assertThat(response.numberOfPassengers()).isEqualTo(2);
        }

        @Test
        @DisplayName("refuses to update a booking that is no longer PENDING")
        void refusesToUpdateConfirmedBooking() {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() ->
                    bookingService.updateBooking(REF, new BookingUpdateRequest(1L, "BUSINESS", null)))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("Can only update PENDING bookings");
        }

        @Test
        @DisplayName("rejects the update when the new cabin lacks capacity")
        void rejectsWhenNewCabinFull() {
            flight.setFirstClassSeats(1);
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

            assertThatThrownBy(() ->
                    bookingService.updateBooking(REF, new BookingUpdateRequest(1L, "FIRST", 5)))
                    .isInstanceOf(InsufficientSeatsException.class)
                    .hasMessageContaining("requested 5");
        }
    }

    // ---- Ownership enforcement ----

    @Nested
    @DisplayName("ownership enforcement")
    class OwnershipEnforcement {

        @Test
        @DisplayName("a customer can read their own booking")
        void customerReadsOwnBooking() {
            authenticateAs(CUSTOMER_EMAIL, "CUSTOMER");
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));

            BookingResponse response = bookingService.getBookingByReference(REF);

            assertThat(response.userEmail()).isEqualTo(CUSTOMER_EMAIL);
        }

        @Test
        @DisplayName("a customer cannot read someone else's booking")
        void customerCannotReadOthersBooking() {
            authenticateAs("someone.else@example.com", "CUSTOMER");
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.getBookingByReference(REF))
                    .isInstanceOf(BookingException.class)
                    .hasMessageContaining("you can only manage your own bookings");
        }

        @Test
        @DisplayName("an admin can read any booking")
        void adminReadsAnyBooking() {
            authenticateAs("admin@airline.com", "ADMIN");
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));

            BookingResponse response = bookingService.getBookingByReference(REF);

            assertThat(response.bookingReference()).isEqualTo(REF);
        }

        @Test
        @DisplayName("a customer cannot cancel someone else's booking")
        void customerCannotCancelOthersBooking() {
            authenticateAs("someone.else@example.com", "CUSTOMER");
            when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(REF))
                    .isInstanceOf(BookingException.class);

            verify(paymentSettlementService, never()).refundIfCharged(any());
            verify(flightRepository, never()).save(any());
        }
    }

    // ---- Queries ----

    @Nested
    @DisplayName("queries")
    class Queries {

        @Test
        @DisplayName("returns every booking belonging to a user")
        void listsBookingsForUser() {
            when(bookingRepository.findByUserId(1L)).thenReturn(List.of(booking));

            List<BookingResponse> results = bookingService.getBookingsByUserId(1L);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().bookingReference()).isEqualTo(REF);
        }

        @Test
        @DisplayName("filters bookings by status, accepting lowercase input")
        void filtersByStatusCaseInsensitively() {
            when(bookingRepository.findByStatus(Booking.BookingStatus.PENDING))
                    .thenReturn(List.of(booking));

            List<BookingResponse> results = bookingService.getBookingsByStatus("pending");

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().status()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("rejects an unrecognised booking status")
        void rejectsUnknownStatus() {
            assertThatThrownBy(() -> bookingService.getBookingsByStatus("NOT_A_STATUS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid booking status");
        }

        @Test
        @DisplayName("returns an empty list when a user has no bookings")
        void returnsEmptyListForUserWithNoBookings() {
            when(bookingRepository.findByUserId(42L)).thenReturn(List.of());

            assertThat(bookingService.getBookingsByUserId(42L)).isEmpty();
        }
    }

    // ---- Response mapping ----

    @Test
    @DisplayName("maps the flight route, timezones and customer email onto the response")
    void mapsResponseFields() {
        when(bookingRepository.findByBookingReference(REF)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.getBookingByReference(REF);

        assertThat(response.flightNumber()).isEqualTo("EI156");
        assertThat(response.departureAirport()).isEqualTo("DUB");
        assertThat(response.arrivalAirport()).isEqualTo("LHR");
        assertThat(response.departureTimezone()).isEqualTo("Europe/Dublin");
        assertThat(response.arrivalTimezone()).isEqualTo("Europe/London");
        assertThat(response.userEmail()).isEqualTo(CUSTOMER_EMAIL);
    }
}
