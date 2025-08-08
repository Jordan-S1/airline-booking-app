/**
 * Represents an airport entity in the airline booking system.
 * This entity stores essential information about airports including their IATA codes,
 * names, locations, and timestamps for auditing purposes.
 *
 * @since 1.0
 */
package com.airlinebookingsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "airports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Airport {

    /**
     * Unique identifier for the airport.
     * Auto-generated value using identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * IATA (International Air Transport Association) code for the airport.
     * This is a unique three-letter code (e.g., "DUB" for Dublin Airport).
     * The code must be unique and cannot be null.
     */
    @Column(unique = true, nullable = false, length = 3)
    private String code;

    /**
     * Official name of the airport.
     * This field cannot be null.
     */
    @Column(nullable = false)
    private String name;

    /**
     * City where the airport is located.
     * This field cannot be null.
     */
    @Column(nullable = false)
    private String city;

    /**
     * Country where the airport is located.
     * This field cannot be null.
     */
    @Column(nullable = false)
    private String country;

    /**
     * Timezone identifier for the airport's location.
     * Represents the time zone in which the airport operates.
     */
    private String timezone;

    /**
     * Timestamp indicating when the airport record was created.
     * Automatically set by Hibernate when the entity is persisted.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the airport record was last updated.
     * Automatically updated by Hibernate when the entity is modified.
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Flights departing from this airport
     */
    @OneToMany(mappedBy = "departureAirport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Flight> departureFlights;

    /**
     * Flights arriving at this airport
     */
    @OneToMany(mappedBy = "arrivalAirport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Flight> arrivalFlights;

}
