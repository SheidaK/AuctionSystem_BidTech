package com.BidTech.auctionSystem.IAMService;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class IAMService {

    private final UserRepository userRepository;

    public IAMService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Signup
    public User RegisterUser(String first_name, String last_name, String userName, String userPassword, String userEmail, String role,String streetNumber, String streetName,String city, String postalCode,String country) {
    	User newUser = new User(first_name, last_name, userName,userEmail, userPassword, role,streetNumber,  streetName, city,  postalCode, country);
    	Optional<User> existingUser =userRepository.findByUserName(newUser.getUserName());
    	
    	if (existingUser.isPresent()) {
    		throw new RuntimeException("User already exists");
        }
        return userRepository.save(newUser);
    }
    // Sign-in
    public boolean AuthenticateUser(String userName, String userEmail, String userPassword) {
    	Optional<User> user = userRepository.findByUserName(userName);
    	Optional<User> user2 = userRepository.findByEmail(userEmail);
    	if (user.isPresent() || user2.isPresent()) {
            return user.get().getPassword().equals(userPassword);
        }
        return false;
    }

    // Password recovery
    public boolean PasswordReset(String userName, String newPassword) {
    	 Optional<User> user = userRepository.findByUserName(userName);
         if (user.isPresent()) {
             User u = user.get();
             u.setPassword(newPassword);
             userRepository.save(u);
             return true;
         }
         return false;    }

    // Role management
    public String GetUserRole(String userName) {
    	Optional<User> user = userRepository.findByUserName(userName);

        return user.map(User::getRole).orElse(null);    }
}