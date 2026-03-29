package com.BidTech.auctionSystem.IAMService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

	private final UserRepository repository;

	UserController(UserRepository repository) {
		this.repository = repository;
	}
	@GetMapping("/users")
	List<User> all() {
		return repository.findAll();
	}

	@PostMapping("/users")
	User newUser(@RequestBody User newUser) {
		return repository.save(newUser);
	}		  
	@GetMapping("/users/{id}")
	User one(@PathVariable Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
	}

	@PutMapping("/users/{id}")
	User replaceUserInformation(@RequestBody User newUser, @PathVariable Long id) {
		    
		return repository.findById(id)
				.map(user -> {
		        user.setFirstName(newUser.getFirstName());
		        user.setLastName(newUser.getLastName());
		        user.setEmail(newUser.getEmail());
				user.setUserName(newUser.getUserName());		        
				user.setRole(newUser.getRole());
				user.setPassword(newUser.getPassword());
				user.setStreetNumber(newUser.getStreetNumber());
				user.setStreetName(newUser.getStreetName());
				user.setCity(newUser.getCity());
				user.setPostalCode(newUser.getPostalCode());
				user.setCountry(newUser.getCountry());
		        return repository.save(user);
		      })
		      .orElseGet(() -> {
		        return repository.save(newUser);
		      });
		  }

	@DeleteMapping("/users/{id}")
	void deleteEmployee(@PathVariable Long id) {
		repository.deleteById(id);
	}
	@GetMapping("/users/usernames/{userName}")
	Optional<User> getUserByUsername(@PathVariable String userName){
	    return repository.findByUserName(userName);
	}
	@PutMapping("/users/reset-password/{userName}")
	public ResponseEntity<?> resetPassword(@PathVariable String userName,@RequestBody Map<String,String> body){
	    return repository.findByUserName(userName)
	    		.map(user -> {
	    			user.setPassword(body.get("password"));
	                repository.save(user);
	                return ResponseEntity.ok("Password updated");
	            })
	            	.orElse(ResponseEntity.notFound().build());
	}
	 @GetMapping("/users/check-username/{userName}")
	    public Map<String, Boolean> checkUsername(@PathVariable String userName) {
	        boolean exists = repository.findByUserName(userName).isPresent();
	        return Map.of("exists", exists);
	    }

	    // --- Check if email exists (for frontend validation) ---
	    @GetMapping("/users/check-email/{email}")
	    public Map<String, Boolean> checkEmail(@PathVariable String email) {
	        boolean exists = repository.findByEmail(email).isPresent();
	        return Map.of("exists", exists);
	    }
}