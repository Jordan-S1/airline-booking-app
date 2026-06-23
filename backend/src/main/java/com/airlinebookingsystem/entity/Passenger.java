package com.airlinebookingsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a passenger entity in the airline booking system.
 * This entity stores essential information about passengers including their personal details,
 * booking information, and seat assignments.
 *
 * @since 1.0
 */
@Entity
@Table(name = "passengers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "booking")
public class Passenger {

    /**
     * Unique identifier for the passenger.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * First name of the passenger.
     */
    @Column(nullable = false)
    private String firstName;

    /**
     * Last name of the passenger.
     */
    @Column(nullable = false)
    private String lastName;

    /**
     * Date of birth of the passenger.
     */
    @Column(nullable = false)
    private LocalDate dateOfBirth;

    /**
     * Gender of the passenger.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    /**
     * Passport number of the passenger.
     */
    @Column(nullable = false)
    private String passportNumber;

    /**
     * Nationality of the passenger.
     */
    @Column(nullable = false)
    private String nationality;

    /**
     * Assigned seat number for the passenger.
     */
    private String seatNumber;

    /**
     * Type of passenger (Adult, Child, or Infant).
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PassengerType passengerType = PassengerType.ADULT;

    /**
     * Associated booking for this passenger.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * Timestamp when the passenger record was created.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp when the passenger record was last updated.
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Enumeration of possible gender values for passengers.
     */
    public enum Gender {
        MALE, FEMALE, OTHER
    }

    /**
     * Enumeration of possible passenger types based on age categories.
     */
    public enum PassengerType {
        ADULT, CHILD, INFANT
    }
}
