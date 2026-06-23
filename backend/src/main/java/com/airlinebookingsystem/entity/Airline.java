package com.airlinebookingsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Represents an airline entity in the airline booking system.
 * This entity stores essential information about airlines including their IATA codes,
 * names, and operational details.
 *
 * @since 1.0
 */

@Entity
@Table(name = "airlines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "flights")
public class Airline {

    /**
     * Unique identifier for the airline.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * IATA (International Air Transport Association) code for the airline.
     * This is a unique two-letter code (e.g., "AA" for American Airlines).
     */
    @Column(unique = true, nullable = false, length = 2)
    private String code;

    /**
     * Official name of the airline.
     */
    @Column(nullable = false)
    private String name;

    /**
     * URL to the airline's logo image.
     */
    private String logoUrl;

    /**
     * Official website URL of the airline.
     */
    private String website;

    /**
     * Country where the airline is registered.
     */
    @Column(nullable = false)
    private String country;

    /**
     * Indicates whether the airline is currently active in the system.
     * Defaults to true for new airlines.
     */
    @Builder.Default
    private Boolean active = true;
    
    /**
     * One-to-many relationship with Flight entities
     * All flights operated by this airline
     * Lazy loading to avoid performance issues when loading airline data
     */
    @OneToMany(mappedBy = "airline", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Flight> flights;

    /**
     * Timestamp indicating when the airline record was created.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the airline record was last updated.
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

