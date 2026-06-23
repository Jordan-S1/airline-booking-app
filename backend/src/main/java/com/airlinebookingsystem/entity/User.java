package com.airlinebookingsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Represents a user entity in the airline booking system.
 * This entity implements Spring Security's UserDetails interface for authentication and authorization.
 * Users can be customers, administrators, or airline staff members.
 *
 * @since 1.0
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "bookings")
public class User implements UserDetails {

    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's email address, used as username for authentication. Must be unique.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Encrypted password for user authentication.
     */
    @Column(nullable = false)
    private String password;

    /**
     * User's first name.
     */
    @Column(nullable = false)
    private String firstName;

    /**
     * User's last name.
     */
    @Column(nullable = false)
    private String lastName;

    /**
     * User's contact phone number.
     */
    private String phoneNumber;

    /**
     * User's street address.
     */
    private String address;

    /**
     * User's city of residence.
     */
    private String city;

    /**
     * User's country of residence.
     */
    private String country;

    /**
     * User's postal code.
     */
    private String postalCode;

    /**
     * User's role in the system (CUSTOMER, ADMIN, or AIRLINE_STAFF).
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    /**
     * Flag indicating if the user account is enabled.
     */
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Flag indicating if the user account has not expired.
     */
    @Builder.Default
    private Boolean accountNonExpired = true;

    /**
     * Flag indicating if the user account is not locked.
     */
    @Builder.Default
    private Boolean accountNonLocked = true;

    /**
     * Flag indicating if the user's credentials have not expired.
     */
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    /**
     * Collection of bookings associated with this user.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Booking> bookings;

    /**
     * Timestamp when the user record was created.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp when the user record was last updated.
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Returns the authorities granted to the user.
     * @return a list containing the user's role as a GrantedAuthority
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Returns the email address used to authenticate the user.
     *
     * @return the user's email address
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indicates whether the user's account has expired.
     *
     * @return true if the user's account is valid (non-expired), false otherwise
     */
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    /**
     * Indicates whether the user is locked or unlocked.
     *
     * @return true if the user is not locked, false otherwise
     */
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     *
     * @return true if the user's credentials are valid (non-expired), false otherwise
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     *
     * @return true if the user is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Defines the possible roles a user can have in the system.
     * CUSTOMER: Regular user who can book flights
     * ADMIN: System administrator with full access
     * AIRLINE_STAFF: Airline employee with specific privileges
     */
    public enum Role {
        CUSTOMER, ADMIN, AIRLINE_STAFF
    }
}
