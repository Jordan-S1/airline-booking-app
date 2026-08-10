package com.airlinebookingsystem.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A signed-in user changing their own password.
 *
 * <p>The current password is required even though the caller is already
 * authenticated: a token can be borrowed from an unlocked machine, and
 * knowing the existing password is the only evidence that whoever is asking
 * is the account holder rather than whoever sat down at their desk.
 */
public record PasswordChangeRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {}
