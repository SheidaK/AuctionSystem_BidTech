package com.BidTech.auctionSystem.IAMService;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * UserRepository — Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides standard CRUD operations plus two custom finder methods for
 * looking up users by username or email address.
 *
 * <p>This repository is bound to the {@code iamEntityManagerFactory} and
 * {@code IAM.db} via {@link com.BidTech.auctionSystem.config.IamDbConfig}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique username.
     *
     * <p>Spring Data JPA generates: {@code SELECT * FROM users WHERE user_name = ?}
     *
     * @param userName the username to search for
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findByUserName(String userName);

    /**
     * Finds a user by their email address.
     *
     * <p>Spring Data JPA generates: {@code SELECT * FROM users WHERE email = ?}
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findByEmail(String email);
}
