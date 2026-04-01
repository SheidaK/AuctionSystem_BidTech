package com.BidTech.auctionSystem.IAMService;

import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * IAMService — Identity and Access Management business logic.
 */
@Service
public class IAMService {

    private final UserRepository userRepository;

    public IAMService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user in the system.
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


    public boolean AuthenticateUser(String userName, String userEmail, String userPassword) {
        Optional<User> user = userRepository.findByUserName(userName);
        Optional<User> user2 = userRepository.findByEmail(userEmail);
        if (user.isPresent() || user2.isPresent()) {
            return user.get().getPassword().equals(userPassword);
        }
        return false;
    }


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

   
    public String GetUserRole(String userName) {
        Optional<User> user = userRepository.findByUserName(userName);
        return user.map(User::getRole).orElse(null);
    }
}