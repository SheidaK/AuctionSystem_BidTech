package com.BidTech.auctionSystem.IAMService;

/**
 * UserNotFoundException — thrown when a user cannot be found by their ID.
 *
 * <p>Thrown by {@link UserController} when a requested user ID does not exist
 * in the database. Spring's default exception handling returns a 500 response;
 * a {@code @ControllerAdvice} handler should map this to 404 in production.
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Creates a new exception identifying the missing user.
     *
     * @param id the ID of the user that was not found
     */
    public UserNotFoundException(Long id) {
        super("Could not find user " + id);
    }
}