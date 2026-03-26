package com.BidTech.auctionSystem.IAMService;

import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * IAMService — Identity and Access Management business logic.
 *
 * <p>This service handles user registration, authentication, password recovery,
 * and role management. It is the authoritative source for user identity operations
 * in the BidTech platform.
 *
 * <p><b>Security note:</b> Passwords are compared in plain text. A production system
 * should use BCrypt hashing via Spring Security's {@code PasswordEncoder}.
 */
@Service
public class IAMService {

    /** Repository for persisting and retrieving {@link User} entities. */
    private final UserRepository userRepository;

    /**
     * Constructor injection of the user repository.
     *
     * @param userRepository the user data access object
     */
    public IAMService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user in the system.
     *
     * <p>Checks that the username is not already taken before saving.
     *
     * @param first_name   the user's first name
     * @param last_name    the user's last name
     * @param userName     the desired unique username
     * @param userPassword the user's password (plain text)
     * @param userEmail    the user's email address
     * @param role         the user's role ("BUYER" or "SELLER")
     * @param streetNumber street number of the shipping address
     * @param streetName   street name of the shipping address
     * @param city         city of the shipping address
     * @param postalCode   postal code of the shipping address
     * @param country      country of the shipping address
     * @return the saved {@link User} entity
     * @throws RuntimeException if the username is already taken
     */
    public User RegisterUser(String first_name, String last_name, String userName,
                             String userPassword, String userEmail, String role,
                             String streetNumber, String streetName, String city,
                             String postalCode, String country) {
        User newUser = new User(first_name, last_name, userName, userEmail, userPassword,
                                role, streetNumber, streetName, city, postalCode, country);

        // Reject if username already exists
        Optional<User> existingUser = userRepository.findByUserName(newUser.getUserName());
        if (existingUser.isPresent()) {
            throw new RuntimeException("User already exists");
        }
        return userRepository.save(newUser);
    }

    /**
     * Authenticates a user by username/email and password.
     *
     * <p>Looks up the user by username first, then by email. Returns {@code true}
     * if the stored password matches the provided password.
     *
     * @param userName     the username to authenticate
     * @param userEmail    the email to authenticate (fallback)
     * @param userPassword the password to verify
     * @return {@code true} if authentication succeeds; {@code false} otherwise
     */
    public boolean AuthenticateUser(String userName, String userEmail, String userPassword) {
        Optional<User> user = userRepository.findByUserName(userName);
        Optional<User> user2 = userRepository.findByEmail(userEmail);
        if (user.isPresent() || user2.isPresent()) {
            return user.get().getPassword().equals(userPassword);
        }
        return false;
    }

    /**
     * Resets a user's password.
     *
     * @param userName    the username of the user whose password to reset
     * @param newPassword the new password (plain text)
     * @return {@code true} if the password was updated; {@code false} if user not found
     */
    public boolean PasswordReset(String userName, String newPassword) {
        Optional<User> user = userRepository.findByUserName(userName);
        if (user.isPresent()) {
            User u = user.get();
            u.setPassword(newPassword);
            userRepository.save(u);
            return true;
        }
        return false;
    }

    /**
     * Returns the role of a user by username.
     *
     * @param userName the username to look up
     * @return the user's role string, or {@code null} if the user does not exist
     */
    public String GetUserRole(String userName) {
        Optional<User> user = userRepository.findByUserName(userName);
        return user.map(User::getRole).orElse(null);
    }
}