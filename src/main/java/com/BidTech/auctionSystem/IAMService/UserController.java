package com.BidTech.auctionSystem.IAMService;

import java.util.List;

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
}