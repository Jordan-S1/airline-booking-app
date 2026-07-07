package com.airlinebookingsystem.dto.user;

/**
 * Request DTO for a user updating their own profile.
 * Excludes role (admin only) and password (separate endpoint).
 */
public record UserUpdateRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        String address,
        String city,
        String country,
        String postalCode
) {}
