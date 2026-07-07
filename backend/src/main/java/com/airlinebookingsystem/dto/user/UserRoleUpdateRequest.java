package com.airlinebookingsystem.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating a user's role.
 * Kept separate from UserRequest to make role changes explicit and auditable.
 */
public record UserRoleUpdateRequest(
        @NotBlank(message = "Role is required")
        String role
) {}
