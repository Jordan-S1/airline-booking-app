package com.airlinebookingsystem.dto.user;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String address,
        String city,
        String country,
        String postalCode,
        String role,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
