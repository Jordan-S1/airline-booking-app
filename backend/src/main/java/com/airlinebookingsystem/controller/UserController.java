package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.user.UserRoleUpdateRequest;
import com.airlinebookingsystem.dto.user.UserUpdateRequest;
import com.airlinebookingsystem.dto.user.UserResponse;
import com.airlinebookingsystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user management.
 * Role access rules:
 *   ADMIN  — full access to all endpoints
 *   AIRLINE_STAFF — read-only access to user list
 *   CUSTOMER — can only view and update their own profile
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Users", description = "User management — view profiles, manage roles and account status")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get all users", description = "ADMIN and AIRLINE_STAFF only")
    @PreAuthorize("hasAnyRole('ADMIN', 'AIRLINE_STAFF')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Get all active users", description = "ADMIN and AIRLINE_STAFF only")
    @PreAuthorize("hasAnyRole('ADMIN', 'AIRLINE_STAFF')")
    @GetMapping("/active")
    public ResponseEntity<List<UserResponse>> getActiveUsers() {
        log.info("GET /users/active");
        return ResponseEntity.ok(userService.getActiveUsers());
    }

    @Operation(summary = "Get users by role", description = "ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(
            @Parameter(description = "Role: CUSTOMER, ADMIN, AIRLINE_STAFF")
            @PathVariable String role) {
        log.info("GET /users/role/{}", role);
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @Operation(summary = "Get user by ID",
            description = "ADMIN can view any user. CUSTOMER can only view their own profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id.equals(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("GET /users/{}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Update user profile",
            description = "ADMIN can update any user. CUSTOMER can only update their own profile. Role changes not allowed here — use PATCH /users/{id}/role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id.equals(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT /users/{}", id);
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @Operation(summary = "Update user role", description = "ADMIN only")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "400", description = "Cannot demote last admin"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        log.info("PATCH /users/{}/role — new role: {}", id, request.role());
        return ResponseEntity.ok(userService.updateUserRole(id, request));
    }

    @Operation(summary = "Deactivate a user account", description = "ADMIN only. Deactivated users cannot log in.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deactivated"),
            @ApiResponse(responseCode = "400", description = "Cannot deactivate last admin"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        log.info("PATCH /users/{}/deactivate", id);
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    @Operation(summary = "Reactivate a user account", description = "ADMIN only")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User reactivated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<UserResponse> reactivateUser(@PathVariable Long id) {
        log.info("PATCH /users/{}/reactivate", id);
        return ResponseEntity.ok(userService.reactivateUser(id));
    }

    @Operation(summary = "Permanently delete a user", description = "ADMIN only")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /users/{}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}