package com.BidTech.auctionSystem.IAMService;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * UserRepository — Spring Data JPA repository for User entities.
 *
 * Provides CRUD operations and custom finder methods for
 * looking up users by username or email address.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique username.
     * Spring Data JPA generates: {@code SELECT * FROM users WHERE user_name = ?}
     * @param userName the username to search for
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findByUserName(String userName);

    /**
     * Finds a user by their email address.
     * Spring Data JPA generates: {@code SELECT * FROM users WHERE email = ?}
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findByEmail(String email);
}
