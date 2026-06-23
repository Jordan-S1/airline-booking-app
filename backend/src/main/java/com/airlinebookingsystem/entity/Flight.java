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
 * Entity representing a flight in the airline booking system.
 * This class contains comprehensive information about a flight, including its schedule,
 * capacity, pricing, and current status. It maintains relationships with airlines,
 * airports, and bookings, while tracking seat availability across different classes.
 *
 * @since 1.0
 */
@Entity
@Table(name = "flights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "bookings")
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_airport_id", nullable = false)
    private Airport departureAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_airport_id", nullable = false)
    private Airport arrivalAirport;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    /**
     * Flight duration in minutes.
     */
    @Column(nullable = false)
    private Integer duration;

    /**
     * Base price for the flight, used as a reference for pricing calculations.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private Integer totalSeats;

    /**
     * Number of seats currently available for booking across all classes.
     */
    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false)
    private Integer economySeats;

    @Column(nullable = false)
    private Integer businessSeats;

    @Column(nullable = false)
    private Integer firstClassSeats;

    /**
     * Price for economy class seats.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal economyPrice;

    /**
     * Price for business class seats.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal businessPrice;

    /**
     * Price for first-class seats.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal firstClassPrice;

    /**
     * Represents the current status of the flight.
     * By default, the status is set to SCHEDULED.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightStatus status = FlightStatus.SCHEDULED;

    /**
     * Indicates whether the flight is active in the system.
     * Inactive flights are hidden from search results but preserved for historical data.
     */
    @Builder.Default
    private Boolean active = true;

    /**
     * Aircraft model or registration number used for this flight.
     */
    private String aircraft;

    /**
     * All bookings associated with this flight.
     */
    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Booking> bookings;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Enum representing the possible states of a flight.
     * SCHEDULED: Flight is planned and on schedule
     * DELAYED: Flight is delayed from its original schedule
     * CANCELLED: Flight has been canceled
     * BOARDING: Passengers are currently boarding
     * DEPARTED: Flight has left the departure airport
     * ARRIVED: Flight has reached its destination
     */
    public enum FlightStatus {
        SCHEDULED, DELAYED, CANCELLED, BOARDING, DEPARTED, ARRIVED
    }
}