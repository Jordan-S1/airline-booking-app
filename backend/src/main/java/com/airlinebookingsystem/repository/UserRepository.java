package com.airlinebookingsystem.repository;

import com.airlinebookingsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing User entities in the database.
 * Provides CRUD operations and custom query methods for users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the user if found, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address to check
     * @return true if a user exists with the given email, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Retrieves all users with a specific role.
     *
     * @param role the role to search for
     * @return a list of users with the specified role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(User.Role role);

    /**
     * Retrieves all active users from the database.
     *
     * @return a list of all active users
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true")
    List<User> findActiveUsers();

    /**
     * Counts users with a specific role.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(User.Role role);

    /**
     * Counts active users with a specific role.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.enabled = true")
    long countActiveByRole(User.Role role);
}
