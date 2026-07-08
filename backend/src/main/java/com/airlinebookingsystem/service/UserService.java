package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.user.UserRoleUpdateRequest;
import com.airlinebookingsystem.dto.user.UserUpdateRequest;
import com.airlinebookingsystem.dto.user.UserResponse;
import com.airlinebookingsystem.entity.User;
import com.airlinebookingsystem.exception.BookingException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getActiveUsers() {
        log.info("Fetching active users");
        return userRepository.findActiveUsers().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(String role) {
        log.info("Fetching users with role: {}", role);
        try {
            User.Role userRole = User.Role.valueOf(role.toUpperCase());
            return userRepository.findByRole(userRole).stream()
                    .map(this::mapToUserResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role + ". Valid roles: CUSTOMER, ADMIN, AIRLINE_STAFF");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(@NonNull Long id) {
        return mapToUserResponse(
                userRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User", id)));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return mapToUserResponse(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("User", email)));
    }

    /**
     * Updates a user's own profile details.
     * Role changes must go through updateUserRole() — ADMIN only.
     */
    public UserResponse updateUser(@NonNull Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.phoneNumber() != null) user.setPhoneNumber(request.phoneNumber());
        if (request.address() != null) user.setAddress(request.address());
        if (request.city() != null) user.setCity(request.city());
        if (request.country() != null) user.setCountry(request.country());
        if (request.postalCode() != null) user.setPostalCode(request.postalCode());

        log.info("Updated profile for user ID: {}", id);
        return mapToUserResponse(userRepository.save(user));
    }

    /**
     * Updates a user's role — ADMIN only.
     * Cannot demote the last admin in the system.
     */
    public UserResponse updateUserRole(@NonNull Long id, UserRoleUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        User.Role newRole;
        try {
            newRole = User.Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + request.role());
        }

        // Prevent demoting the last admin
        if (user.getRole() == User.Role.ADMIN && newRole != User.Role.ADMIN) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new BookingException("Cannot demote the last admin in the system");
            }
        }

        log.info("Updating role for user ID: {} from {} to {}", id, user.getRole(), newRole);
        user.setRole(newRole);
        return mapToUserResponse(userRepository.save(user));
    }

    /**
     * Deactivates a user account — ADMIN only.
     * Deactivated users cannot log in.
     */
    public UserResponse deactivateUser(@NonNull Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (!user.isEnabled()) {
            throw new BookingException("User account is already deactivated");
        }
        if (user.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.countActiveByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new BookingException("Cannot deactivate the last active admin");
            }
        }

        user.setEnabled(false);
        log.info("Deactivated user ID: {}", id);
        return mapToUserResponse(userRepository.save(user));
    }

    /**
     * Reactivates a deactivated user account — ADMIN only.
     */
    public UserResponse reactivateUser(@NonNull Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.isEnabled()) {
            throw new BookingException("User account is already active");
        }

        user.setEnabled(true);
        log.info("Reactivated user ID: {}", id);
        return mapToUserResponse(userRepository.save(user));
    }

    /**
     * Permanently deletes a user — ADMIN only.
     */
    public void deleteUser(@NonNull Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getCity(),
                user.getCountry(),
                user.getPostalCode(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
