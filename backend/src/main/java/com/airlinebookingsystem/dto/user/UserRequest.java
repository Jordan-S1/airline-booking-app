package com.airlinebookingsystem.dto.user;

public record UserRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        String phoneNumber,
        String address,
        String city,
        String country,
        String postalCode,
        String role
) {}
