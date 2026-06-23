package com.airlinebookingsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Represents a booking entity in the airline booking system.
 * This entity stores information about flight reservations, including passenger details,
 * payment information, and booking status.
 *
 * @since 1.0
 */
@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"passengers", "payment"})
public class Booking {

    /**
     * Unique identifier for the booking.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique reference number for the booking.
     */
    @Column(unique = true, nullable = false)
    private String bookingReference;

    /**
     * User who made the booking.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Flight that was booked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    /**
     * Total number of passengers in the booking.
     */
    @Column(nullable = false)
    private Integer numberOfPassengers;

    /**
     * Total amount paid for the booking.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Current status of the booking.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    /**
     * Class of seats booked.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatClass seatClass;

    /**
     * Set of passengers included in this booking.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Passenger> passengers;

    /**
     * Payment information associated with the booking.
     */
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Payment payment;

    /**
     * Timestamp when the booking was created.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp when the booking was last updated.
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Represents the possible states of a booking.
     * PENDING: Initial state when booking is created
     * CONFIRMED: Booking is confirmed after successful payment
     * CANCELLED: Booking has been canceled
     * COMPLETED: Booking is completed after flight
     */
    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }

    /**
     * Represents the available seat classes for a booking.
     * ECONOMY: Standard class seating
     * BUSINESS: Business class seating
     * FIRST: First-class seating
     */
    public enum SeatClass {
        ECONOMY, BUSINESS, FIRST
    }
}