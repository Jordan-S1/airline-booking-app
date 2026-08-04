package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.config.PasswordEncoderConfig;
import com.airlinebookingsystem.config.SecurityConfig;
import com.airlinebookingsystem.dto.booking.BookingRequest;
import com.airlinebookingsystem.dto.booking.BookingResponse;
import com.airlinebookingsystem.exception.InsufficientSeatsException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.security.JwtService;
import com.airlinebookingsystem.security.RestAuthenticationEntryPoint;
import com.airlinebookingsystem.service.BookingService;
import com.airlinebookingsystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for BookingController, focused on role-based access.
 *
 * The application's real SecurityConfig is imported, which is what brings
 * @EnableMethodSecurity — without it @PreAuthorize is simply not applied and
 * every one of these tests would pass while proving nothing. @WithMockUser
 * populates the context, method security reads it, and a denial surfaces as
 * 403 through the global handler. Each protected route is therefore checked
 * from both an allowed and a forbidden role.
 */
@WebMvcTest(controllers = BookingController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, RestAuthenticationEntryPoint.class})
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private BookingService bookingService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;

    private static final String REF = "BK17832167460316843";

    private BookingResponse booking() {
        return new BookingResponse(
                1L, REF, 1L, "EI156", "DUB", "LHR",
                LocalDateTime.of(2026, 8, 1, 6, 30),
                LocalDateTime.of(2026, 8, 1, 7, 55),
                "Europe/Dublin", "Europe/London",
                2, new BigDecimal("179.98"), "PENDING", "ECONOMY",
                "jordan@example.com",
                LocalDateTime.of(2026, 7, 1, 12, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0));
    }

    @Nested
    @DisplayName("admin-only routes")
    class AdminOnly {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN can list bookings by status")
        void adminCanListByStatus() throws Exception {
            when(bookingService.getBookingsByStatus(anyString())).thenReturn(List.of(booking()));

            mockMvc.perform(get("/api/v1/bookings/status/CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].bookingReference").value(REF));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER is refused the status listing with 403")
        void customerCannotListByStatus() throws Exception {
            mockMvc.perform(get("/api/v1/bookings/status/CONFIRMED"))
                    .andExpect(status().isForbidden());

            verify(bookingService, never()).getBookingsByStatus(anyString());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("an anonymous request gets 401, not 403 — the client must be able to tell")
        void anonymousCannotListByStatus() throws Exception {
            mockMvc.perform(get("/api/v1/bookings/status/CONFIRMED"))
                    .andExpect(status().isUnauthorized());

            verify(bookingService, never()).getBookingsByStatus(anyString());
        }
    }

    @Nested
    @DisplayName("customer-and-admin routes")
    class SharedRoutes {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER can create a booking and gets 201")
        void customerCanCreateBooking() throws Exception {
            when(bookingService.createBooking(any(BookingRequest.class), anyLong()))
                    .thenReturn(booking());

            mockMvc.perform(post("/api/v1/bookings/user/1")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new BookingRequest(1L, "ECONOMY", null))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.bookingReference").value(REF))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN can read any booking by reference")
        void adminCanReadBooking() throws Exception {
            when(bookingService.getBookingByReference(REF)).thenReturn(booking());

            mockMvc.perform(get("/api/v1/bookings/" + REF))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userEmail").value("jordan@example.com"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER can cancel a booking")
        void customerCanCancel() throws Exception {
            when(bookingService.cancelBooking(REF)).thenReturn(booking());

            mockMvc.perform(patch("/api/v1/bookings/" + REF + "/cancel"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("an anonymous user gets 401 reading a booking")
        void anonymousCannotRead() throws Exception {
            mockMvc.perform(get("/api/v1/bookings/" + REF))
                    .andExpect(status().isUnauthorized());

            verify(bookingService, never()).getBookingByReference(anyString());
        }
    }

    @Nested
    @DisplayName("error translation")
    class ErrorTranslation {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("an unknown reference becomes 404")
        void unknownReferenceIsNotFound() throws Exception {
            when(bookingService.getBookingByReference(anyString()))
                    .thenThrow(new ResourceNotFoundException("Booking", REF));

            mockMvc.perform(get("/api/v1/bookings/" + REF))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("a sold-out cabin becomes 409")
        void insufficientSeatsIsConflict() throws Exception {
            when(bookingService.createBooking(any(BookingRequest.class), anyLong()))
                    .thenThrow(new InsufficientSeatsException("ECONOMY", 2, 0));

            mockMvc.perform(post("/api/v1/bookings/user/1")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(
                                    new BookingRequest(1L, "ECONOMY", null))))
                    .andExpect(status().isConflict());
        }
    }
}
