package com.airlinebookingsystem.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Redeems a reset token. The new password is held to the same rule as registration. */
public record ResetPasswordRequest(

        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {}
