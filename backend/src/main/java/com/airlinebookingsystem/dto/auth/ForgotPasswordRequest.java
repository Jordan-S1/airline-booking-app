package com.airlinebookingsystem.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Asks for a reset link. Answered identically whether or not the address exists. */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {}
